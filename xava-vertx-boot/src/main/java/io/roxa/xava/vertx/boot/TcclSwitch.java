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
package io.roxa.xava.vertx.boot;

import java.util.concurrent.Callable;

/**
 * @author Steven Chen
 *
 */
public class TcclSwitch {
	public static <T> T executeWithTCCLSwitch(Callable<T> action) throws Exception {
		final ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(TcclSwitch.class.getClassLoader());
			return action.call();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	public static void executeWithTCCLSwitch(Runnable action) {
		final ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(TcclSwitch.class.getClassLoader());
			action.run();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}
}
