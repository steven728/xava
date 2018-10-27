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
package io.roxa.xava.vertx.boot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

/**
 * @author Steven Chen
 *
 */
@Component(immediate = true, service = {})
public class VerticleDeployer {

	private static final Logger logger = LoggerFactory.getLogger(VerticleDeployer.class);

	private Vertx vertxService;

	private List<Verticle> verticles = new ArrayList<>();

	private Map<Verticle, String> deployedVerticles = new ConcurrentHashMap<>();

	@Deactivate
	public void stop() {
		logger.info("Stopping deployer and remove verticles.");
		cleanup();
	}

	@Reference(unbind = "unsetVertxService", policy = ReferencePolicy.STATIC)
	public void setVertxService(Vertx vertxService) {
		this.vertxService = vertxService;
		if (!verticles.isEmpty()) {
			logger.info("Deploy verticle from verticles.");
			verticles.forEach(verticle -> this.vertxService.deployVerticle(verticle));
		}
	}

	@Reference(unbind = "removeVerticle", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
	public void addVerticle(Verticle verticle) throws IOException {
		if (verticle == null)
			return;
		verticles.add(verticle);
		if (vertxService != null)
			vertxService.deployVerticle(verticle, deploy -> {
				if (deploy.succeeded()) {
					logger.info("Deployment of verticle {} with id:{} succeeded.", verticle.getClass().getName(),
							deploy.result());
					String id = deploy.result();
					deployedVerticles.put(verticle, id);
				} else {
					logger.error("Deployment of verticle failed.", deploy.cause());
				}
			});
	}

	public void unsetVertxService(Vertx vertxService) {
		cleanup();
		this.vertxService = null;
	}

	public void removeVerticle(Verticle verticle) {
		if (verticle == null)
			return;
		logger.info("Undeploying verticle {}.", verticle.getClass().getName());
		verticles.remove(verticle);
		if (vertxService != null && deployedVerticles.get(verticle) != null) {
			String verticleId = deployedVerticles.get(verticle);
			if (vertxService.deploymentIDs().contains(verticleId)) {
				vertxService.undeploy(verticleId);
			}
			deployedVerticles.remove(verticle);
		}
	}

	private void cleanup() {
		if (vertxService != null) {
			verticles.forEach(verticle -> {
				String deploymentID = verticle.getVertx().getOrCreateContext().deploymentID();
				if (deploymentID != null)
					vertxService.undeploy(deploymentID);
			});
		}

	}

}
