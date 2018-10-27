/**
 * The MIT License
 * 
 * Copyright (c) 2018-2020 Shell Technologies PTY LTD
 *
 * You may obtain a copy of the License at
 * 
 *       http://mit-license.org/
 *       
 */
package io.roxa.xava.jqm.internal;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.GeneralFailureException;
import io.roxa.xava.jqm.Job;
import io.roxa.xava.jqm.JobState;
import io.roxa.xava.jqm.JobStorage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.redis.RedisTransaction;

/**
 * @author Steven Chen
 *
 */
public class JobStorageImpl implements JobStorage {

	private static final Logger logger = LoggerFactory.getLogger(JobStorageImpl.class);

	private RedisClient cli;

	private Vertx vertx;

	private String host;

	/**
	 * 
	 */
	public JobStorageImpl(Vertx vertx, String host) {
		this.vertx = vertx;
		this.host = host;
	}

	public Future<Job> create(String catalog) {
		return Future.succeededFuture(Job.create(catalog));
	}

	public Future<Job> save(Job job) {
		Future<Job> future = Future.future();
		if (job.getId() != null) {
			return update(job);
		}
		getRedisClient().incr(StorageKeys.keyAutoIncrement(), ar -> {
			if (ar.failed()) {
				future.fail(ar.cause());
			} else {
				Long incrId = ar.result();
				job.setId(incrId);
				job.setScoreId(incrId);
				job.setState(JobState.INACTIVE);
				future.complete(job);
			}
		});
		return future.compose(j -> {
			if (j.getDelay() > 0) {
				j.setState(JobState.DELAYED);
			}
			Long now = System.currentTimeMillis();
			j.setDateCreated(now).setDatePromoted(now + j.getDelay());
			getRedisClient().sadd(StorageKeys.keyForJobCatalogs(), j.getCatalog(), redisOperationFailed());
			return update(j);
		});
	}

	public Future<Optional<Job>> fetch(String scoreId) {
		Future<Optional<Job>> future = Future.future();
		Long id = Job.resolveScoreId(scoreId);
		getRedisClient().hgetall(StorageKeys.keyForJobInfo(id), ar -> {
			if (ar.succeeded()) {
				JsonObject jo = ar.result();
				if (jo == null)
					future.complete(Optional.<Job>empty());
				else
					future.complete(Optional.of(Job.create(ar.result())));
			} else {
				future.fail(ar.cause());
			}
		});
		return future;
	}

	public Future<Job> update(Job job) {
		Future<Job> future = Future.future();
		Long dateFailed = job.getDateFailed();
		Long millis = (dateFailed != 0) ? dateFailed : job.getDateCreated();
		job.setDatePromoted(millis + job.getDelay());
		job.setDateUpdated(System.currentTimeMillis());
		// TODO: removeOnComplete, backoff
		getRedisClient().zadd(StorageKeys.keyForJobs(), job.getPriorityValue(), job.getScroeId(),
				redisOperationFailed());
		getRedisClient().hmset(StorageKeys.keyForJobInfo(job.getId()), job.getUpdatedInfo(), ar -> {
			if (ar.succeeded()) {
				future.complete(job);
			} else {
				future.fail(ar.cause());
			}
		});
		return future.compose(j -> transState(j, job.getState()));
	}

	public Future<Void> remove(Job job) {
		Future<Void> future = Future.future();
		String scoreId = job.getScroeId();
		JobState state = job.getState();
		getRedisClient().transaction().multi(redisOperationFailed())
				.zrem(StorageKeys.keyForState(state), scoreId, redisOperationFailed())
				.zrem(StorageKeys.keyForState(job.getCatalog(), state), scoreId, redisOperationFailed())
				.zrem(StorageKeys.keyForJobs(), scoreId, redisOperationFailed())
				.del(StorageKeys.keyForJobLog(job.getId()), redisOperationFailed())
				.del(StorageKeys.keyForJobInfo(job.getId()), redisOperationFailed()).exec(ar -> {
					if (ar.succeeded()) {
						// TODO: emit remove event: job.id, job.catalog, remove
						future.complete();
					} else {
						future.fail(ar.cause());
					}
				});
		return future;
	}

	public Future<Job> transState(Job job, JobState newState) {
		Future<Job> future = Future.future();
		RedisClient scli = getNewRedisClient();
		JobState oldState = job.getState();
		String scoreId = job.getScroeId();
		Long jobId = job.getId();
		String catalog = job.getCatalog();
		logger.info("trans state for {}, {} from {} to {}", jobId, scoreId, oldState, newState);
		RedisTransaction tx = scli.transaction().multi(redisOperationFailed());
		if (!oldState.equals(job.getState())) {
			tx.zrem(StorageKeys.keyForState(oldState), scoreId, redisOperationFailed())
					.zrem(StorageKeys.keyForState(catalog, oldState), scoreId, redisOperationFailed());
		}
		JsonObject updateInfo = new JsonObject();
		updateInfo.put("state", newState).put("date_updated", System.currentTimeMillis());
		tx.hmset(StorageKeys.keyForJobInfo(jobId), updateInfo, redisOperationFailed())
				.zadd(StorageKeys.keyForState(newState), job.getPriorityValue(), scoreId, redisOperationFailed())
				.zadd(StorageKeys.keyForState(catalog, oldState), job.getPriorityValue(), scoreId,
						redisOperationFailed());
		// use date_promoted as score when job moves to delayed
		switch (newState) {
		case ACTIVE:
			tx.zadd(StorageKeys.keyForState(JobState.ACTIVE), job.getActivePriorityValue(), scoreId,
					redisOperationFailed());
			break;
		case DELAYED:
			tx.zadd(StorageKeys.keyForState(JobState.DELAYED), job.getDatePromoted(), scoreId, redisOperationFailed());
			break;
		case INACTIVE:
			tx.lpush(StorageKeys.keyForJobList(catalog), "1", redisOperationFailed());
			break;
		default:
			break;
		}
		job.setState(newState);
		tx.exec(ar -> {
			if (ar.succeeded()) {
				// TODO: emit enqueue event: job.id, job.catalog, enqueue
				future.complete(job);
			} else {
				future.fail(ar.cause());
			}
		});
		return future;
	}

	public Future<Optional<Job>> topJob(String catalog) {
		Future<Optional<Job>> future = Future.future();
		getRedisClient().blpop(StorageKeys.keyForJobList(catalog), 0, ar -> {
			if (ar.failed()) {
				getRedisClient().lpush(StorageKeys.keyForJobList(catalog), "1", redisOperationFailed());
			} else {
				takeInactiveJobScroeId(catalog).compose(this::fetch).setHandler(future.completer());
			}
		});
		return future;
	}

	protected Future<String> takeInactiveJobScroeId(String catalog) {
		Future<String> future = Future.future();
		String key = StorageKeys.keyForState(catalog, JobState.INACTIVE);
		getRedisClient().transaction().multi(redisOperationFailed()).zrange(key, 0, 0, redisOperationFailed())
				.zremrangebyrank(key, 0, 0, future).exec(ar -> {
					if (ar.succeeded()) {
						JsonArray rs = ar.result();
						if (rs.getJsonArray(0).size() == 0) {
							future.fail(new GeneralFailureException("No element found"));
						} else {
							future.complete(rs.getJsonArray(0).getString(0));
						}
					} else {
						future.fail(ar.cause());
					}
				});
		return future;
	}

	private RedisClient getNewRedisClient() {
		return RedisClient.create(vertx, new RedisOptions().setHost(host));
	}

	private RedisClient getRedisClient() {
		if (cli == null) {
			cli = RedisClient.create(vertx, new RedisOptions().setHost(host));
		}
		return cli;
	}

	/**
	 * @return
	 */
	private <T> Handler<AsyncResult<T>> redisOperationFailed() {
		return ar -> {
			if (ar.failed()) {
				logger.error("Redis operation failed", ar.cause());
			}
		};
	}

}
