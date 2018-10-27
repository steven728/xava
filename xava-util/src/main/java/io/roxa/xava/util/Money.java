/**
 * The MIT License
 * 
 * Copyright (c) 2016-2016 Shell Technologies PTY LTD
 *
 * You may obtain a copy of the License at
 * 
 *       http://mit-license.org/
 *       
 */
package io.roxa.xava.util;

import java.math.BigDecimal;

/**
 * @author steven
 *
 */
public class Money {
	private static final BigDecimal CURRENCY_FACTOR_100 = new BigDecimal("100");
	private static int DECIMALS = 2;
	private static int ROUNDING_MODE = BigDecimal.ROUND_HALF_UP;

	public static BigDecimal asCurrency(String currencyStr) {
		String _cur = Strings.emptyAsNull(currencyStr);
		if (_cur == null)
			return BigDecimal.ZERO;
		try {
			return rounded(new BigDecimal(_cur));
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	public static BigDecimal asCurrencyFactor100(String currencyStr) {
		String _cur = Strings.emptyAsNull(currencyStr);
		if (_cur == null)
			return BigDecimal.ZERO;
		try {
			BigDecimal _temp = rounded(new BigDecimal(_cur));
			if (_temp.abs().compareTo(CURRENCY_FACTOR_100) < 0)
				return BigDecimal.ZERO;
			return _temp.divide(CURRENCY_FACTOR_100, ROUNDING_MODE);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private static BigDecimal rounded(BigDecimal aNumber) {
		return aNumber.setScale(DECIMALS, ROUNDING_MODE);
	}

}
