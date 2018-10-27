/**
 * The MIT License
 * 
 * Copyright (c) 2016 Shell Technologies PTY LTD
 *
 * You may obtain a copy of the License at
 * 
 *       http://mit-license.org/
 *       
 */
package io.roxa.xava.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;
import java.util.Random;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * @author steven
 *
 */
public class DESedeCipher {

	public static void main(String[] args) {
//		String plainContent = "{\"token\":\"rZdqgkoXAa1537954690\",\"customer_id\":\"5981596756965326737\",\"sign\":\"48FC610852C4A9D1235E00A3456B718B\",\"card_no\":\"00001\"}";
//		String plainIV = "Kl7ZgtM1";
//		String encryptedContent = "uKdXzUXuUqXgXoUcCEOGidN3kG8F8DY3B1b4LNWLoM7fpEnfDUdibUUM0TaVxCNdSGPJZ3NAtTh/xLdfHam5AwwOJjr7WNgXn7VcA/GSAn1iTNq/kH2iOiXN5yTXndQmmeCs5KmcuC+PXPYMqetd1W4GorqOL+4H0OhqcBd2fRLcjmmOMFCQKQ==";
//		DESedeCipher cipher = new DESedeCipher(false);
//		String[] content = cipher.cbc().pkcs5().plainIV("Kl7ZgtM1").key("1E0D0886D2FD1D68321CCD60")
//				.content(encryptedContent, true).doFinal();
//		System.out.println(content[0] + "," + content[1]);
//		content = new DESedeCipher(false).cbc().pkcs5().plainIV(plainIV).key("1E0D0886D2FD1D68321CCD60")
//				.content(plainContent, false).doFinal();
//		System.out.println(Codecs.urlEncode(content[0]) + "," + content[1]);

		String cardNo = "46789001";
		String[] result = new DESedeCipher(false).cbc().pkcs5().plainIV("Kl7ZgtM1").key("1E0D0886D2FD1D68321CCD60")
				.content(cardNo, false).doFinal();
		System.out.println(result[0] + "," + result[1]);
		System.out.println(result[0].hashCode() + "," + result[1]);
		String numStr = result[0].chars().mapToObj(i -> String.format("%03d", i)).collect(Collectors.joining());
		BigInteger bigI = new BigInteger(numStr);

	}

	private static final String randomStrBase = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final String algorithm = "DESede";
	private String feedback;
	private String padding;
	private Key desdedKey;
	private IvParameterSpec ivSpec;
	private byte[] content;
	private Cipher cipher;
	private boolean encrypt = true;
	private Base64.Decoder base64Decoder;
	private Base64.Encoder base64Encoder;
	private String ivSpecString;

	public DESedeCipher(boolean urlSafe) {
		if (urlSafe) {
			base64Decoder = Base64.getUrlDecoder();
			base64Encoder = Base64.getUrlEncoder();
		} else {
			base64Decoder = Base64.getDecoder();
			base64Encoder = Base64.getEncoder();
		}
	}

	public DESedeCipher cbc() {
		this.feedback = "CBC";
		return this;
	}

	public DESedeCipher pkcs5() {
		this.padding = "PKCS5Padding";
		return this;
	}

	public DESedeCipher pkcs7() {
		this.padding = "PKCS7Padding";
		return this;
	}

	public DESedeCipher base64Key(String base64Key) {
		try {
			byte[] keyBytes = base64Decoder.decode(base64Key);
			DESedeKeySpec spec = new DESedeKeySpec(keyBytes);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm);
			desdedKey = keyFactory.generateSecret(spec);
			return this;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public DESedeCipher key(String key) {
		try {
			byte[] keyBytes = key.getBytes();
			DESedeKeySpec spec = new DESedeKeySpec(keyBytes);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm);
			desdedKey = keyFactory.generateSecret(spec);
			return this;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public DESedeCipher plainIV(String ivString) {
		this.ivSpecString = ivString;
		byte[] ivBytes = ivSpecString.getBytes();
		ivSpec = new IvParameterSpec(ivBytes);
		return this;
	}

	public String ivString() {
		if (ivSpec == null) {
			ivSpecString = randomString(8);
			byte[] ivBytes = generateIVBytes(ivSpecString);
			ivSpec = new IvParameterSpec(ivBytes);
		}
		return ivSpecString;
	}

	public DESedeCipher base64IV(String base64IV) {
		byte[] ivBytes = base64Decoder.decode(base64IV);
		ivSpec = new IvParameterSpec(ivBytes);
		return this;
	}

	public DESedeCipher content(String content, boolean encrypted, String charset) {
		encrypt = !encrypted;
		try {
			if (encrypt)
				this.content = content.getBytes(charset);
			else
				this.content = base64Decoder.decode(content.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public DESedeCipher content(String content, boolean encrypted) {
		return content(content, encrypted, "UTF-8");
	}

	public String[] doFinal() {
		try {
			Cipher cipher = getCipher();
			String ivStr = ivString();
			cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, desdedKey, ivSpec);
			byte[] result = cipher.doFinal(content);
			String resultStr = encrypt ? base64Encoder.encodeToString(result) : new String(result);
			return new String[] { resultStr, ivStr };
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	protected Cipher getCipher() throws GeneralSecurityException {
		if (cipher == null)
			cipher = Cipher.getInstance(String.join("/", algorithm, feedback, padding));
		return cipher;
	}

	public static String generateKeyBase64URLSafe() throws GeneralSecurityException {
		return Base64.getUrlEncoder().encodeToString(generateKey());
	}

	public static String randomIVBytesBase64URLSafe(int len) {
		return Base64.getUrlEncoder().encodeToString(randomIVBytes(len));
	}

	public static String generateKeyBase64() throws GeneralSecurityException {
		return Base64.getEncoder().encodeToString(generateKey());
	}

	public static String randomIVBytesBase64(int len) {
		return Base64.getEncoder().encodeToString(randomIVBytes(len));
	}

	public static byte[] generateKey() throws GeneralSecurityException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
		keyGenerator.init(168);
		SecretKey skey = keyGenerator.generateKey();
		return skey.getEncoded();
	}

	public static byte[] randomIVBytes(int len) {
		byte[] ivBytes = new byte[len];
		Random r = new Random();
		r.nextBytes(ivBytes);
		return ivBytes;
	}

	public static byte[] generateIVBytes(String code) {
		byte[] result = new byte[code.length()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) (code.charAt(i) - 48);
		}
		return result;
	}

	public static String randomString(int len) {
		Random random = new Random();
		char[] buf = new char[len];
		for (int i = 0; i < len; i++) {
			int n = random.nextInt(randomStrBase.length());
			buf[i] = randomStrBase.charAt(n);
		}
		return new String(buf);
	}
}
