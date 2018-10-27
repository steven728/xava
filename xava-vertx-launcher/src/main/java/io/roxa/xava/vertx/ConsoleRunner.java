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
package io.roxa.xava.vertx;

import java.util.function.Consumer;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

/**
 * @author Steven Chen
 *
 */
public class ConsoleRunner {

	public static void go(Verticle verticle) {
		simplyRun(verticle);
	}

	private static void simplyRun(Verticle verticle) {
		System.setProperty("vertx.cwd", System.getProperty("user.dir"));
		Consumer<Vertx> runner = vertx -> {
			vertx.deployVerticle(verticle, ar -> {
				if (ar.succeeded()) {
					System.out.printf("Deploy verticle: %s/n", ar.result());
				} else {
					System.err.printf("Could not deploy verticle: %s/n", verticle.getClass().getName());
					ar.cause().printStackTrace();
				}
			});
		};
		Vertx vertx = Vertx.vertx();
		runner.accept(vertx);
	}

}
