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
package io.roxa.xava.vertx.redisource;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.vertx.BaseVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.RedisDataSource;

/**
 * @author Steven Chen
 *
 */
@interface RedisConfig {
	String host() default "redisd";
}

@Component(immediate = true, service = Verticle.class, configurationPid = { "io.roxa.redis" })
public class RedisDataSourcePublisher extends BaseVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RedisDataSourcePublisher.class);

	private RedisConfig redisConfig;

	@Activate
	public void start(BundleContext bundleContext, RedisConfig redisConfig) {
		this.redisConfig = redisConfig;
	}

	public void start(Future<Void> startFuture) throws Exception {
		String redisHost = getRediHost();
		setupServiceDiscovery(null).compose(d -> {
			Record record = RedisDataSource.createRecord("vertx-service:default-redis",
					new JsonObject().put("host", redisHost), new JsonObject());
			logger.info("Preparing to publish redis data source with host:{}", redisHost);
			return publish(record);
		}).setHandler(startFuture.completer());
	}

	/**
	 * @return
	 */
	private String getRediHost() {
		String host = System.getenv("REDIS_HOST");
		if (host == null)
			host = redisConfig.host();
		return host == null ? "localhost" : host;
	}
}
