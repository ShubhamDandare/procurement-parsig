package com.kpmg.rcm.sourcing.common.util;

import java.io.Closeable;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Harsh Goswami
 *
 */
@Slf4j
public class StreamUtil {

	public static void closeStreams(Closeable... streamArray) {
		if (streamArray == null || streamArray.length <= 0)
			return;

		for (Closeable stream : streamArray) {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					log.warn("Error while closing stream: ", e);
				}
			}
		}
	}

	public static void closeStreams(AutoCloseable... streamArray) {
		if (streamArray == null || streamArray.length <= 0)
			return;

		for (AutoCloseable stream : streamArray) {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e) {
					log.warn("Error while closing stream: ", e);
				}
			}
		}
	}

}
