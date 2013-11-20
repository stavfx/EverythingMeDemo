package com.stavfx.quiktweet.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Simpler Base64 encoding/decoding with better compatibility (below API 8)
 */
public class Base64 {

	static final char[] charTab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	public static String encode(byte[] data) {
		return encode(data, 0, data.length, null).toString();
	}

	public static String Decode(String data) {
		try {
			return new String(decode(data), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Encodes the part of the given byte array denoted by start and
	 * len to the Base64 format.  The encoded data is appended to the
	 * given StringBuilder. If no StringBuilder is given, a new one is
	 * created automatically. The StringBuilder is the return value of
	 * this method.
	 */
	public static StringBuilder encode(
			byte[] data,
			int start,
			int len,
			StringBuilder buf) {

		if (buf == null) {
			buf = new StringBuilder(data.length * 3 / 2);
		}
		int end = len - 3;
		int i = start;
		int n = 0;

		while (i <= end) {
			int d =
					((((int) data[i]) & 0x0ff) << 16) | ((((int) data[i + 1]) & 0x0ff) << 8) | (((int) data[i + 2]) & 0x0ff);

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append(charTab[(d >> 6) & 63]);
			buf.append(charTab[d & 63]);

			i += 3;

			if (n++ >= 14) {
				n = 0;
				buf.append("\r\n");
			}
		}

		if (i == start + len - 2) {
			int d =
					((((int) data[i]) & 0x0ff) << 16) | ((((int) data[i + 1]) & 255) << 8);

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append(charTab[(d >> 6) & 63]);
			buf.append("=");
		} else if (i == start + len - 1) {
			int d = (((int) data[i]) & 0x0ff) << 16;

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append("==");
		}

		return buf;
	}

	static int decode(char c) {

		if (c >= 'A' && c <= 'Z') {
			return ((int) c) - 65;
		} else if (c >= 'a' && c <= 'z') {
			return ((int) c) - 97 + 26;
		} else if (c >= '0' && c <= '9') {
			return ((int) c) - 48 + 26 + 26;
		} else {
			switch (c) {
				case '+':
					return 62;
				case '/':
					return 63;
				case '=':
					return 0;
				default:
					throw new RuntimeException(
							"unexpected code: " + c);
			}
		}
	}

	/**
	 * Decodes the given Base64 encoded String to a new byte array.
	 * The byte array holding the decoded data is returned.
	 */
	public static byte[] decode(String s) {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {

			decode(s, bos);

		} catch (IOException e) {
			throw new RuntimeException();
		}
		return bos.toByteArray();
	}

	public static void decode(String s, OutputStream os)
			throws IOException {
		int i = 0;

		int len = s.length();

		while (true) {
			while (i < len && s.charAt(i) <= ' ') {
				i++;
			}
			if (i == len) {
				break;
			}
			int tri =
					(decode(s.charAt(i)) << 18) + (decode(s.charAt(i + 1)) << 12) + (decode(s.charAt(i + 2)) << 6) + (decode(s.charAt(i + 3)));


			os.write((tri >> 16) & 255);
			if (s.charAt(i + 2) == '=') {
				break;
			}
			os.write((tri >> 8) & 255);
			if (s.charAt(i + 3) == '=') {
				break;
			}
			os.write(tri & 255);

			i += 4;
		}
	}
}

