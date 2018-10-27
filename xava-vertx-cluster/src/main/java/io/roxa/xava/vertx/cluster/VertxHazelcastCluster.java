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
package io.roxa.xava.vertx.cluster;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;

import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * @author Steven Chen
 *
 */
@Component(immediate = true, service = {})
public class VertxHazelcastCluster {

	private static final Logger logger = LoggerFactory.getLogger(VertxHazelcastCluster.class);

	private BundleContext bc;

	@Reference
	private HazelcastInstance hazelcastInstance;

	private ServiceRegistration<ClusterManager> clusterManagerService;

	@Activate
	public void start(BundleContext bundleContext) {
		this.bc = bundleContext;
		logger.info("Starting VertxHazelcastCluster ... registering ClusterManager as service.");
		ClusterManager mgr = new HazelcastClusterManager(hazelcastInstance);
		clusterManagerService = bc.registerService(ClusterManager.class, mgr, null);
	}

	@Deactivate
	public void stop() {
		clusterManagerService.unregister();
	}

}
