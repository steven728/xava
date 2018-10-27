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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.micrometer.MetricsService;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;

/**
 * @author Steven Chen
 *
 */
@Component(immediate = true, service = {})
public class VertxBoot {

	private static final Logger logger = LoggerFactory.getLogger(VertxBoot.class);

	private BundleContext bundleContext;
	private Vertx vertx;
	private List<ServiceRegistration<?>> serviceRegistrations;

	@Reference(bind = "bindClusterManager", unbind = "unbindClusterManager", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile ClusterManager clusterManager;

	@Reference
	private VertxMetricsFactory metricsFactory;

	@Activate
	public void start(BundleContext context) {
		logger.info("Vert.x activating...");
		this.bundleContext = context;
		serviceRegistrations = new ArrayList<>();
		setup().setHandler(ar -> {
			if (ar.succeeded()) {
				logger.info("Vert.x started with {}, metrics enabled: {}",
						vertx.isClustered() ? "clustered" : "non clustered", vertx.isMetricsEnabled());
			} else {
				logger.error("Vert.x could not init vertx instance", ar.cause());
			}
		});
	}

	@Deactivate
	public void stop(BundleContext context) {
		logger.info("Vert.x deactivating...");
		teardown().setHandler(ar -> {
			if (ar.succeeded()) {
				logger.info("Vert.x closed.");
			} else {
				logger.error("Vert.x closed error!", ar.cause());
			}
		});
	}

	public void bindClusterManager(ClusterManager clusterManager) {
		this.clusterManager = clusterManager;
		logger.info("Vert.x bind clusterManager.");
	}

	public void unbindClusterManager(ClusterManager clusterManager) {
		logger.info("Vert.x unbind clusterManager.");
		this.clusterManager = null;
	}

	private Future<Vertx> setup() {
		VertxOptions options = new VertxOptions().setMetricsOptions(createMetricsOptions());
		Consumer<Vertx> consumer = v -> {
			serviceRegistrations.add(bundleContext.registerService(Vertx.class, vertx, null));
			serviceRegistrations.add(bundleContext.registerService(EventBus.class, vertx.eventBus(), null));
			// MetricRegistry registry =
			// SharedMetricRegistries.getOrCreate("xava-vertx-registry");
			// serviceRegistrations.add(bundleContext.registerService(MetricRegistry.class,
			// registry, null));
			// MetricsService metricsService = MetricsService.create(vertx);
			MeterRegistry registry = BackendRegistries.getDefaultNow();
			serviceRegistrations.add(bundleContext.registerService(MeterRegistry.class, registry, null));
			MetricsService metricsService = MetricsService.create(vertx);
			serviceRegistrations.add(bundleContext.registerService(MetricsService.class, metricsService, null));
		};
		if (clusterManager != null)
			return bootWithCluster(options, consumer);
		return bootWithoutCluster(options, consumer);

	}

//	protected MetricsOptions createMetricsOptions() {
//		return new DropwizardMetricsOptions().setEnabled(true).setJmxEnabled(true).setJmxDomain("xava-vertx-metrics")
//				.setRegistryName("xava-vertx-registry").setFactory(metricsFactory);
//	}

	private MetricsOptions createMetricsOptions() {
		return new MicrometerMetricsOptions().setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
				.setEnabled(true).setFactory(metricsFactory);
	}

	private Future<Void> teardown() {
		Future<Void> future = Future.future();
		releaseServiceRegistrations();
		if (vertx != null) {
			vertx.close(future.completer());
		} else {
			future.complete();
		}
		return future;
	}

	private void releaseServiceRegistrations() {
		if (serviceRegistrations == null || serviceRegistrations.isEmpty())
			return;
		serviceRegistrations.stream().map(sr -> Optional.ofNullable(sr)).forEach(opt -> {
			opt.ifPresent(target -> target.unregister());
		});
		serviceRegistrations.clear();
	}

	private Future<Vertx> bootWithCluster(VertxOptions options, Consumer<Vertx> consumer) {
		options.setClusterManager(clusterManager);
		try {
			return TcclSwitch.executeWithTCCLSwitch(() -> {
				Future<Vertx> future = Future.future();
				Vertx.clusteredVertx(options, ar -> {
					if (ar.succeeded()) {
						vertx = ar.result();
						consumer.accept(vertx);
						future.complete(vertx);
					} else {
						future.fail(ar.cause());
					}
				});
				return future;
			});
		} catch (Throwable e) {
			return Future.failedFuture(e);
		}
	}

	private Future<Vertx> bootWithoutCluster(VertxOptions options, Consumer<Vertx> consumer) {
		try {
			vertx = TcclSwitch.executeWithTCCLSwitch(() -> Vertx.vertx(options));
			consumer.accept(vertx);
			return Future.succeededFuture(vertx);
		} catch (Throwable e) {
			return Future.failedFuture(e);
		}
	}
}
