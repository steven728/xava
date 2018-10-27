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
package io.roxa.xava.jqm;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author Steven Chen
 *
 */
public interface JobQueue {

	/**
	 * @param vertx
	 * @return
	 */
	static JobQueue create(Vertx vertx) {
		return null;
	}

	/**
	 * @param data
	 * @param catalog
	 * @return
	 */
	Job newJob(String catalog, JsonObject data);

	/**
	 * 
	 */
	void startJob(String catalog, Handler<Job> handler);

	/**
	 * @param job
	 */
	void enqueue(Job job);

}
