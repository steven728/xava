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
package io.roxa.xava.vertx.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.vertx.BaseVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.RedisDataSource;

/**
 * @author Steven Chen
 *
 */
public class ServicePublisher extends BaseVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ServicePublisher.class);

	private String redisHost;

	/**
	 * @param redisHost the redisHost to set
	 */
	public void setRedisHost(String redisHost) {
		this.redisHost = redisHost;
	}

	public void start(Future<Void> startFuture) throws Exception {
		setupServiceDiscovery(null).compose(d -> {
			Future<Void> future = Future.future();
			Record record = RedisDataSource.createRecord("vertx-service:default-redis",
					new JsonObject().put("host", redisHost), new JsonObject());
			discovery.publish(record, ar -> {
				if (ar.succeeded()) {
					logger.info("Redis datasource published.");
					future.complete();
				} else {
					logger.error("Could not publish redis datasource!");
					future.fail(ar.cause());
				}
			});
			return future;
		}).setHandler(startFuture.completer());
	}
}
