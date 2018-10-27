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
package io.roxa.xava.vertx.sample.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.vertx.http.RestAPIProxy;
import io.roxa.xava.vertx.sample.proxy.RestSampleAPI;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author Steven Chen
 *
 */
@Component
public class RestSampleAPIProxy extends RestAPIProxy implements RestSampleAPI {

	private static final Logger logger = LoggerFactory.getLogger(RestSampleAPIProxy.class);

	public RestSampleAPIProxy() {
		super("xava.vertx.endpoint.sample");
		logger.info("RestSampleAPIProxy for xava.vertx.endpoint.sample");
	}

	@Reference
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		setupCircuitBreaker();
	}

	public Future<JsonObject> health() {
		return get("/sample/health");
	}

	public Future<JsonObject> mysql() {
		return get("/sample/mysql");
	}

	public Future<JsonObject> redis() {
		return get("/sample/redis");
	}

}
