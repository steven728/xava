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

/**
 * @author Steven Chen
 *
 */
public enum JobPriority {

	LOW(10), NORMAL(0), MEDIUM(-5), HIGH(-10), CRITICAL(-15);

	private int intValue;

	JobPriority(int intValue) {
		this.intValue = intValue;
	}

	public int intValue() {
		return intValue;
	}

}
