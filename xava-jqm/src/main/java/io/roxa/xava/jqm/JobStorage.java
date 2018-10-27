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

import io.roxa.xava.util.Jndis;
import io.vertx.core.Future;

/**
 * @author Steven Chen
 *
 */
public interface JobStorage {

	static JobStorage instance() {
		return Jndis.service(JobStorage.class);
	}

	Future<Job> create(String catalog);

	Future<Job> save(Job job);

}
