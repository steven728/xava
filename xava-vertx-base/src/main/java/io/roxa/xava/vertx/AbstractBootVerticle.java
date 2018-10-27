/**
 * The MIT License
 * 
 * Copyright (c) 2016-2018 Shell Technologies PTY LTD
 *
 * You may obtain a copy of the License at
 * 
 *       http://mit-license.org/
 *       
 */
package io.roxa.xava.vertx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.fn.Tuple2;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;

/**
 * @author Steven Chen
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractBootVerticle extends BaseVerticle {

	private static final Logger logger = LoggerFactory.getLogger(AbstractBootVerticle.class);

	private final Map<String, String> deploymentIds = new ConcurrentHashMap<>();

	protected Map<String, String> deploymentClasses;

	protected List<Verticle> deploymentInstances;

	/**
	 * 
	 */
	public AbstractBootVerticle() {
	}

	/**
	 * @param deploymentClasses the deploymentClasses to set
	 */
	public void setDeploymentClasses(Map<String, String> deploymentClasses) {
		this.deploymentClasses = deploymentClasses;
	}

	/**
	 * @param deploymentInstances the deploymentInstances to set
	 */
	public void setDeploymentInstances(List<Verticle> deploymentInstances) {
		this.deploymentInstances = deploymentInstances;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		List<Future> futures = new ArrayList<>();
		futures.addAll(deployIntances());
		futures.addAll(deployClasses());
		CompositeFuture.all(futures).setHandler(ar -> {
			if (ar.succeeded()) {
				startFuture.complete();
			} else {
				startFuture.fail(ar.cause());
			}
		});

	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		tearDownServiceDiscovery().setHandler(ar -> {
			// nothing to do!!
		});
		if (deploymentIds == null || deploymentIds.isEmpty()) {
			stopFuture.complete();
			return;
		}
		CompositeFuture.all(deploymentIds.keySet().stream().filter(id -> vertx.deploymentIDs().contains(id))
				.map(this::undeploy).collect(Collectors.toList())).setHandler(ar -> {
					if (ar.succeeded()) {
						stopFuture.complete();
					} else {
						stopFuture.fail(ar.cause());
					}
				});
	}

	protected Future<Void> undeploy(String verticleId) {
		Future<Void> future = Future.future();
		String name = deploymentIds.remove(verticleId);
		if (name == null)
			return Future.succeededFuture();

		vertx.undeploy(verticleId, ar -> {
			if (ar.succeeded()) {
				logger.info("Undeploy verticle succeeded with id: {}, name: {}", verticleId, name);
				future.complete();
			} else {
				logger.error("Undeploy verticle failed with id: {}, name: {}", verticleId, name);
				future.fail(ar.cause());
			}
		});
		return future;
	}

	protected Future<String> deploy(Class<Verticle> verticleClass, int numberOfInstance) {
		if (verticleClass == null)
			return Future.failedFuture("Verticle class is null or not found!");
		Future<String> future = Future.future();
		String name = verticleClass.getName();
		vertx.deployVerticle(verticleClass, new DeploymentOptions().setInstances(numberOfInstance), ar -> {
			if (ar.succeeded()) {
				String id = ar.result();
				logger.info("Deployed verticle: {}, Id: {}, number of instance: {}", name, id, numberOfInstance);
				deploymentIds.put(id, name);
				future.complete(ar.result());
			} else {
				logger.error("Cannot deploy verticle: " + name, ar.cause());
				future.fail(ar.cause());
			}
		});
		return future;
	}

	protected Future<Void> deploy(Verticle verticle) {
		if (verticle == null)
			return Future.failedFuture("The verticle instance must not be null!");
		Future<Void> future = Future.future();
		String name = verticle.getClass().getName();
		vertx.deployVerticle(verticle, ar -> {
			if (ar.succeeded()) {
				String id = ar.result();
				logger.info("Deployed verticle: {}, Id: {}", name, id);
				deploymentIds.put(id, name);
				future.complete();
			} else {
				logger.error("Cannot deploy verticle: " + name, ar.cause());
				future.fail(ar.cause());
			}
		});
		return future;
	}

	private List<Future<Void>> deployIntances() {
		if (deploymentInstances == null || deploymentInstances.isEmpty())
			return new ArrayList<>();
		return deploymentInstances.stream().map(this::deploy).collect(Collectors.toList());
	}

	private List<Future<String>> deployClasses() {
		if (deploymentClasses == null || deploymentClasses.isEmpty())
			return new ArrayList<>();
		return deploymentClasses.entrySet().stream().map(e -> {
			String verticle = e.getKey();
			int numberOfInstance = 0;
			try {
				numberOfInstance = Integer.parseInt(e.getValue());
			} catch (Exception e1) {
			}
			if (numberOfInstance <= 0 || numberOfInstance > 10)
				numberOfInstance = 1;
			Class<Verticle> clazz = loadClass(verticle);
			return new Tuple2<Class<Verticle>, Integer>(clazz, numberOfInstance);
		}).map(tuple -> {
			return deploy(tuple.$0(), tuple.$1());
		}).collect(Collectors.toList());
	}

	private Class<Verticle> loadClass(String className) {
		try {
			return (Class<Verticle>) this.getClass().getClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {

		}
		return null;
	}

}
