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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Verticle;

/**
 * @author Steven Chen
 *
 */
@SuppressWarnings("rawtypes")
public class VerticleExtender {

	private static final Logger logger = LoggerFactory.getLogger(VerticleExtender.class);
	private Bundle verticleBundle;
	private List<ServiceRegistration<Verticle>> verticleServices;
	private BundleContext verticleBundleContext;

	private List<Class> verticles;

	public VerticleExtender(Bundle bundle, List<Class> verticles) {
		this.verticleBundle = bundle;
		verticleBundleContext = bundle.getBundleContext();
		verticleServices = new ArrayList<>();
		this.verticles = verticles;
	}

	public void start() {
		logger.debug("VerticleExtender started for bundle {}", verticleBundle);
		logger.debug("found {} verticles", verticles.size());

		List<Verticle> verticleInstances = verticles.stream().map(verticleClass -> {
			Verticle verticle = null;
			try {
				verticle = (Verticle) verticleClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				// munch
			}
			return verticle;
		}).collect(Collectors.toList());

		logger.debug("created {} Verticle instances", verticleInstances.size());

		verticleServices = verticleInstances.stream()
				.map(verticle -> verticleBundleContext.registerService(Verticle.class, verticle, null))
				.collect(Collectors.toList());

		logger.info("Registered {} Verticle services", verticleServices.size());
	}

	public void destroy() {
		logger.debug("destroying extender for bundle {}", verticleBundle);
		verticleServices.stream().forEach(registration -> registration.unregister());
	}
}
