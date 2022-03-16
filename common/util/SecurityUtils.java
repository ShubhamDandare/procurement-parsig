package com.kpmg.rcm.sourcing.common.util;

import org.apache.commons.codec.digest.DigestUtils;

public class SecurityUtils {

	public static String SHA256Hash(String arg) {
		String hash = DigestUtils.sha256Hex(arg);
		if (hash != null && !hash.isEmpty()) {
			return hash.toUpperCase();
		}
		return null;
	}
}
