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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.vertx.sample.proxy.RestSampleAPI;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * @author Steven Chen
 *
 */
@Command(scope = "xava-vertx", name = "health", description = "The Sample RESTful API Health check")
@Service
public class HealthCheck implements Action {

	private static final Logger logger = LoggerFactory.getLogger(HealthCheck.class);
	@Argument(index = 0, name = "type", description = "type of health should be checked, [http, mysql, redis]", required = true, multiValued = false)
	String type = "http";
	@Reference
	private RestSampleAPI restSampleAPI;

	@Override
	public Object execute() throws Exception {
		Future<JsonObject> future = null;
		switch (type) {
		case "http":
			future = restSampleAPI.health();
			System.out.println("The [http] health check has been executed, see the log for details.");
			break;
		case "mysql":
			future = restSampleAPI.mysql();
			System.out.println("The [mysql] health check has been executed, see the log for details.");
			break;
		case "redis":
			future = restSampleAPI.redis();
			System.out.println("The [redis] health check has been executed, see the log for details.");
			break;
		default:
			System.err.println("Illegal type: " + type);
			break;
		}
		if (future != null)
			future.setHandler(ar -> {
				if (ar.succeeded()) {
					logger.info("Health check result: {}", ar.result().encodePrettily());
				} else {
					logger.error("Failed", ar.cause());
				}
			});
		return null;
	}

}
