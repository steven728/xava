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
package io.roxa.xava.vertx.monitor;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.roxa.xava.vertx.http.AbstractHttpVerticle;
import io.vertx.circuitbreaker.HystrixMetricHandler;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.MetricsService;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;

/**
 * @author Steven Chen
 *
 */
@interface MonitorServerConfig {
	int port() default 8219;
}

@Component(immediate = true, service = { Verticle.class }, configurationPid = "io.roxa.monitor-server")
public class MonitorServer extends AbstractHttpVerticle {

	private MetricsService metricsService;

	private Router router;

	private MonitorServerConfig config;

	public MonitorServer() {
		setContextPath("/monitor");
		setStaticServingLocation("/pages");
	}

	@Activate
	public void activate(BundleContext context, MonitorServerConfig config) {
		this.config = config;
		setPort(this.config.port());
	}

	@Reference
	public void bindMetricsService(MetricsService metricsService) {
		this.metricsService = metricsService;
	}

	protected String getServerName() {
		return "Monitor Server";
	}

	protected Future<Router> setupRouter(Router router) {
//		BridgeOptions options = new BridgeOptions()
//				.addOutboundPermitted(new PermittedOptions().setAddress("xava.vertx.monitor.metrics"));
//		router.route(composePath("/eventbus/*")).handler(SockJSHandler.create(vertx).bridge(options));
		router.get(composePath("/overview")).produces(MEDIA_TYPE_APPLICATION_JSON).handler(this::overview);
		router.get(composePath("/metrics")).produces(MEDIA_TYPE_APPLICATION_JSON).handler(this::metrics);
		router.get(composePath("/metrics/:id")).produces(MEDIA_TYPE_APPLICATION_JSON).handler(this::metricsWithId);
		router.route(composePath("/prometheus")).handler(rc -> {
			PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
			if (prometheusRegistry != null) {
				String response = prometheusRegistry.scrape();
				rc.response().end(response);
			} else {
				rc.fail(500);
			}
		});
		router.get(composePath("/circuit-metrics")).handler(HystrixMetricHandler.create(vertx));
//		vertx.setPeriodic(60 * 1000, t -> {
//			JsonObject metrics = metricsService.getMetricsSnapshot(vertx.eventBus());
//			vertx.eventBus().publish("xava.vertx.monitor.metrics", metrics);
//		});
		this.router = router;
		return super.setupRouter(router);
	}

	protected Future<Void> setupHttpEndpoint(ServiceDiscovery discovery) {
		ServiceDiscoveryRestEndpoint.create(router, discovery, composePath("/discovery"));
		return super.setupHttpEndpoint(discovery);
	}

	private void overview(RoutingContext rc) {
		JsonArray deployedVerticles = vertx.deploymentIDs().stream().map(id -> {
			Deployment deployment = ((VertxInternal) vertx).getDeployment(id);
			JsonObject deploymentInfo = new JsonObject().put("id", id)
					.put("verticle_identifier", deployment.verticleIdentifier())
					.put("options", deployment.deploymentOptions().toJson());
			return deploymentInfo;
		}).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
		succeeded(rc, deployedVerticles);
	}

	private void metrics(RoutingContext rc) {
		succeeded(rc, metricsService.metricsNames().stream().map(i -> new JsonObject().put("id", i))
				.collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
	}

	private void metricsWithId(RoutingContext rc) {
		String metricsId = requestParam(rc, "id");
		JsonObject metricsSnapshot = metricsService.getMetricsSnapshot(metricsId).getJsonObject(metricsId);
		String type = metricsSnapshot.getString("type");
		if (type.equalsIgnoreCase("histogram") || type.equalsIgnoreCase("timer")) {
			metricsSnapshot.put("percentile75", metricsSnapshot.getValue("75%"));
			metricsSnapshot.put("percentile95", metricsSnapshot.getValue("95%"));
			metricsSnapshot.put("percentile98", metricsSnapshot.getValue("98%"));
			metricsSnapshot.put("percentile99", metricsSnapshot.getValue("99%"));
			metricsSnapshot.put("percentile999", metricsSnapshot.getValue("99.9%"));
		}
		succeeded(rc, new JsonObject().put("id", metricsId).put("snapshot", metricsSnapshot));
	}

}
