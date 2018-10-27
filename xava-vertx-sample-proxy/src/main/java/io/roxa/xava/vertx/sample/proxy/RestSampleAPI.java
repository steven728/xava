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
package io.roxa.xava.vertx.sample.proxy;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * @author Steven Chen
 *
 */
public interface RestSampleAPI {

	Future<JsonObject> health();

	Future<JsonObject> mysql();

	Future<JsonObject> redis();

}
