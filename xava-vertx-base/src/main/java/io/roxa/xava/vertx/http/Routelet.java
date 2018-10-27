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
package io.roxa.xava.vertx.http;

import io.vertx.ext.web.Router;

/**
 * @author Steven Chen
 *
 */
public interface Routelet {

	Router getRouter();

}
