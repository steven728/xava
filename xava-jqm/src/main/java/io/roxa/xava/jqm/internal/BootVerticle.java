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
package io.roxa.xava.jqm.internal;

import io.roxa.xava.jqm.JobStorage;
import io.roxa.xava.util.Jndis;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * @author Steven Chen
 *
 */
public class BootVerticle extends AbstractVerticle {

	public void start(Future<Void> startFuture) throws Exception {
		JobStorage inst = new JobStorageImpl(vertx, "redisd");
		Jndis.registry(JobStorage.class.getName(), inst);
		super.start(startFuture);
	}

	public void stop(Future<Void> stopFuture) throws Exception {
		Jndis.unregistry(JobStorage.class.getName());
		super.stop(stopFuture);
	}

}
