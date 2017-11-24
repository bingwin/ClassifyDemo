package com.example.admin.classifydemo;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * �������ݵ�AES���ܣ�����AES_KEYΪ��Կ
 * 
 * @author Administrator
 * 
 */
public class AESUtils {
	private final static String HEX = "0123456789ABCDEF";
	private static final String CBC_PKCS5_PADDING = "AES/CBC/NoPadding";// AES�Ǽ��ܷ�ʽ
																		// CBC�ǹ���ģʽ
																		// PKCS5Padding�����ģʽ
	private static final String AES = "AES";// AES ����
	private static final String SHA1PRNG = "SHA1PRNG";// // SHA1PRNG ǿ��������㷨,
														// Ҫ����4.2���ϰ汾�ĵ��÷���

	private final static String AES_IV = "a14521b6c96266hg";
	private final static String AES_KEY = "09f5e8f7fc1a0d27";

	/**
	 * ���ڲ����ֽڣ�ȷ���ֽ���Ϊ16�ı���
	 * 
	 * @param src
	 * @return
	 */
	public static byte[] multiple(byte[] src) {
		int len = src.length;
		int n = len % 16;
		byte[] ret = new byte[src.length + 16 - n];

		for (int i = 0; i < src.length; i++) {
			ret[i] = src[i];
		}

		for (int i = src.length; i < (16 - n); i++) {
			ret[i] = '\0';
		}
		return ret;
	}

	/**
	 * byte[]תhex
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}

		return stringBuilder.toString();
	}

	/**
	 * 16�����ַ���תbyte[]
	 * 
	 * @param hexString
	 * @return
	 */
	public static byte[] hexStringToBytes(String hexString) {
		try {
			if (hexString == null || hexString.equals("")) {
				return null;
			}
			hexString = hexString.toUpperCase();
			int length = hexString.length() / 2;
			char[] hexChars = hexString.toCharArray();
			byte[] result = new byte[length];
			for (int i = 0; i < length; i++) {
				int pos = i * 2;
				result[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
			}
			return result;
		} catch (Exception e) {

		}
		return null;

	}

	/**
	 * charתbyte
	 * 
	 * @param c
	 * @return
	 */
	public static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	/**
	 * AES�����ַ���
	 * 
	 * @param content
	 *            ��Ҫ�����ܵ��ַ���
	 * @param password
	 *            ������Ҫ������
	 * @return ����
	 */
	public static byte[] encrypt(String content) {
		try {
			// KeyGenerator kgen = KeyGenerator.getInstance("AES"); //
			// ����AES��Key������
			// kgen.init(128, new SecureRandom(AES_KEY.getBytes()));//
			// �����û�������Ϊ�������ʼ����
			// //128λ��key������
			// //����û��ϵ��SecureRandom�����ɰ�ȫ��������У�password.getBytes()�����ӣ�ֻҪ������ͬ�����о�һ�������Խ���ֻҪ��password����
			// SecretKey secretKey = kgen.generateKey();// �����û����룬����һ����Կ
			// byte[] enCodeFormat = secretKey.getEncoded();//
			// ���ػ��������ʽ����Կ���������Կ��֧�ֱ��룬�򷵻�null��
			// SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");//
			// ת��ΪAESר����Կ
			SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);// ����������
			byte[] byteContent = content.getBytes("utf-8");
			byteContent = multiple(byteContent);
			cipher.init(Cipher.ENCRYPT_MODE, key,
					new IvParameterSpec(AES_IV.getBytes()));// ��ʼ��Ϊ����ģʽ��������
			byte[] result = cipher.doFinal(byteContent);// ����

			// return Base64.encodeBase64(result);
			return result;

		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * ��������������Ե�����̬����Կ ���ܺͽ��ܵ���Կ����һ�£���Ȼ�����ܽ���
	 */
	public static String generateKey() {
		try {
			SecureRandom localSecureRandom = SecureRandom.getInstance(SHA1PRNG);
			byte[] bytes_key = new byte[20];
			localSecureRandom.nextBytes(bytes_key);
			String str_key = toHex(bytes_key);
			return str_key;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// ����Կ���д���
	private static byte[] getRawKey(byte[] seed) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance(AES);
		// for android
		SecureRandom sr = null;
		// ��4.2���ϰ汾�У�SecureRandom��ȡ��ʽ�����˸ı�
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			sr = SecureRandom.getInstance(SHA1PRNG, "Crypto");
		} else {
			sr = SecureRandom.getInstance(SHA1PRNG);
		}
		// for Java
		// sr = SecureRandom.getInstance(SHA1PRNG);
		sr.setSeed(seed);
		kgen.init(128, sr); // 256 bits or 128 bits,192bits
		// AES��128λ��Կ�汾��10������ѭ����192������Կ�汾��12������ѭ����256������Կ�汾����14������ѭ����
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();
		return raw;
	}

	/*
	 * ����
	 */
	public static byte[] decrypt(byte[] encrypted) throws Exception {
		// byte[] raw = getRawKey(key.getBytes());
		// SecretKeySpec skeySpec = new SecretKeySpec(raw, AES);
		// encrypted = Base64.decodeBase64(encrypted);
		SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
		cipher.init(Cipher.DECRYPT_MODE, skeySpec,
				new IvParameterSpec(AES_IV.getBytes()));
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}

	// ������ת�ַ�
	public static String toHex(byte[] buf) {
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2 * buf.length);
		for (int i = 0; i < buf.length; i++) {
			appendHex(result, buf[i]);
		}
		return result.toString();
	}

	private static void appendHex(StringBuffer sb, byte b) {
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}

}
