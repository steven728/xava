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

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.util.Randoms;
import io.roxa.xava.vertx.JdbcExecutor;
import io.roxa.xava.vertx.RedisSource;
import io.roxa.xava.vertx.http.AbstractHttpVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * @author Steven Chen
 *
 */
@Component(immediate = true, service = { Verticle.class })
public class RestSampleAPIServer extends AbstractHttpVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RestSampleAPIServer.class);

	@Reference
	private DataSource dataSource;

	private JdbcExecutor jdbcExecutor;

	private Long timerId;

	public RestSampleAPIServer() {
		setPort(8200);
		setContextPath("/sample");
	}

	@Override
	protected String getServerName() {
		return "REST sample server";
	}

	protected Future<Void> setupHttpEndpoint(ServiceDiscovery discovery) {
		return publishHttpEndpoint("xava.vertx.endpoint.sample", "api.sample", port);
	}

	protected Future<Void> setupResources() {
		jdbcExecutor = new JdbcExecutor(JDBCClient.create(vertx, dataSource));
		vertx.eventBus().consumer("xava-vertx.sample.testcase1.start", msg -> {
			logger.info("Starting the test case 1...");
			startTestCase1();
		});
		vertx.eventBus().consumer("xava-vertx.sample.testcase1.stop", msg -> {
			logger.info("Stopping the test case 1...");
			stopTestCase1();
		});
		return super.setupResources();
	}

	protected Future<Router> setupRouter(Router router) {
		router.get(composePath("/mysql")).handler(rc -> {
			jdbcExecutor.query("select 1", null).setHandler(ar -> {
				if (ar.succeeded()) {
					succeeded(rc, new JsonObject().put("mysql", ar.result().get(0).getInteger(0)));
				} else {
					failed(rc, ar.cause());
				}
			});
		});
		router.get(composePath("/testcase1")).handler(rc -> {
			vertx.eventBus().send("xava-vertx.sample.testcase1.start", new JsonObject());
			succeeded(rc);
		});
		router.delete(composePath("/testcase1")).handler(rc -> {
			vertx.eventBus().send("xava-vertx.sample.testcase1.stop", new JsonObject());
			succeeded(rc);
		});
		router.get(composePath("/redis")).handler(rc -> {
			RedisSource.exec(discovery, cli -> {
				Future<String> future = Future.future();
				cli.ping(future.completer());
				return future;
			}).setHandler(ar -> {
				if (ar.succeeded()) {
					succeeded(rc, new JsonObject().put("redis", ar.result()));
				} else {
					failed(rc, ar.cause());
				}
			});
		});
		return super.setupRouter(router);
	}

	/**
	 * create table test_case_t1 (id int not null primary key auto_increment,
	 * text_info varchar(64) not null);
	 * <p>
	 * create table test_case_t2 (id int not null primary key auto_increment,
	 * text_info varchar(64) not null);
	 * <p>
	 * create table test_case_t3 (id int not null primary key auto_increment,
	 * text_info varchar(64) not null);
	 */
	private void startTestCase1() {
		timerId = vertx.setPeriodic(2000, i -> {
			bizMethod().compose(v -> {
				logger.info("do other biz");
				Integer.parseInt("z");
				return Future.failedFuture("To Make a fail");
			}).setHandler(ar -> {
				if (ar.succeeded()) {
					logger.info("The test case 1 executed.");
				} else {
					logger.error("The test case 1 failed.", ar.cause());
				}
			});
		});
		logger.info("Started the timerId:{}", timerId);
	}

	private Future<Integer> bizMethod() {
		return jdbcExecutor.execTx(conn -> {
			return jdbcExecutor.update(conn, "insert into test_case_t1(text_info) values(?)", getSample("t1"))
					.compose(uc -> {
						return jdbcExecutor.update(conn, "insert into test_case_t2(text_info) values(?)",
								getSample("t2"));
					}).compose(uc -> {
						return jdbcExecutor.update(conn, "insert into test_case_t3(text_info) values(?)",
								getSample("t3"));
					});
		});
	}

	/**
	 * 
	 */
	private void stopTestCase1() {
		if (timerId != null) {
			vertx.cancelTimer(timerId);
			logger.info("Stopped the timerId:{}", timerId);
		}
	}

	private JsonArray getSample(String type) {
		return new JsonArray().add(Randoms.randomString(32));
	}

}
