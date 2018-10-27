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
package io.roxa.xava.vertx.internal;

import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * <p>
 * Setup the system property <code>io.roxa.vertx.cluster=true</code> to launcher
 * vertx cluster model. The defualt is false for standalone model
 * </p>
 * 
 * @author Steven Chen
 */
public class VertxLauncher implements ServiceTrackerCustomizer<Verticle, Verticle> {

	private static final Logger logger = LoggerFactory.getLogger(VertxLauncher.class);

	private Vertx vertx;

	private ConcurrentHashMap<Verticle, String> deploymentIds = new ConcurrentHashMap<>();

	private ServiceTracker<Verticle, Verticle> tracker;

	private BundleContext bundleContext;

	private boolean isCluster;

	/**
	 * 
	 */
	public VertxLauncher() {

	}

	public void setBundleContext(BundleContext context) throws Exception {
		bundleContext = context;

	}

	@Override
	public Verticle addingService(ServiceReference<Verticle> reference) {
		Verticle verticle = bundleContext.getService(reference);
		if (vertx == null) {
			logger.info("Vertx intance is not ready, deployment is pending...");
			deploymentIds.put(verticle, "PENDING");
		} else {
			deployVerticle(verticle);
		}
		return verticle;
	}

	@Override
	public void modifiedService(ServiceReference<Verticle> reference, Verticle service) {
		logger.info("Service modified: {}", service.getClass().getName());
	}

	@Override
	public void removedService(ServiceReference<Verticle> reference, Verticle verticle) {
		if (verticle == null)
			return;
		String id = deploymentIds.remove(verticle);
		// Already undeployed?
		if (id != null && vertx.deploymentIDs().contains(id)) {
			vertx.undeploy(id);
			logger.info("Undeploying verticle: {}, Id: {}", verticle.getClass().getName(), id);
		}

	}

	public void start() {
		isCluster = Boolean.getBoolean("io.roxa.vertx.cluster");
		if (isCluster)
			startWithClustered();
		else
			startWithNonClustered();
	}

	public void stop() {
		if (vertx != null)
			vertx.close(ar -> {
				if (ar.succeeded()) {
					logger.info("Vertx close is complete.");
				} else {
					Throwable t = ar.cause();
					logger.error(t.getMessage());
				}
			});
		if (tracker != null)
			tracker.close();
	}

	protected void deployVerticle(Verticle verticle) {
		vertx.deployVerticle(verticle, ar -> {
			if (ar.succeeded()) {
				deploymentIds.put(verticle, ar.result());
				logger.info("Deployed verticle: {}, Id: {}", verticle.getClass().getName(), ar.result());
			} else {
				logger.error("Cannot deploy verticle: " + verticle.getClass().getName(), ar.cause());
			}
		});
	}

	protected void startWithNonClustered() {
		vertx = Vertx.vertx();
		logger.info("Vertx is a non clustered instance using default options");
		tracker = new ServiceTracker<>(bundleContext, Verticle.class, this);
		tracker.open();
	}

	protected void startWithClustered() {
		ClusterManager mgr = new HazelcastClusterManager();
		VertxOptions options = new VertxOptions().setClusterManager(mgr);
		Vertx.clusteredVertx(options, ar -> {
			if (ar.succeeded()) {
				vertx = ar.result();
				deploymentIds.keySet().stream().filter(v -> deploymentIds.get(v).equals("PENDING"))
						.forEach(this::deployVerticle);
				logger.info("Vertx is a hazelcast clustered instance using default options");
			} else {
				logger.error("Could not initial clustered vertx instance");
			}
		});
		tracker = new ServiceTracker<>(bundleContext, Verticle.class, this);
		tracker.open();
	}
}
