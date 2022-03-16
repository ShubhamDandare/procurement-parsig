package com.kpmg.rcm.sourcing.common.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * {@code RHSException} is the superclass of those exceptions that can be thrown
 * during the normal operation of the KPMG RHS components.
 */
public class RHSException extends RuntimeException {
	private static final long serialUID = 1L;

	String exceptionCode;
	String exceptionMessage;
	static String name = "RHSException";

	/**
	 * Constructs a new exception with the specified exception code and detail
	 * message.
	 *
	 * @param code    the exception code.
	 * @param message the detail message.
	 * @param args    additional arguments to be replaced in the message.
	 */
	public RHSException(String code, String message, Object... args) {
		super(String.format(message, args));
		exceptionCode = code;
		exceptionMessage = String.format(message, args);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param code  the exception code.
	 * @param cause the cause.
	 */
	public RHSException(String code, Throwable cause) {
		super(cause);
		exceptionCode = code;
		exceptionMessage = cause.getMessage();
	}

	/**
	 * Constructs a new exception with the specified exception code, cause and
	 * detail message.
	 *
	 * @param code    the exception code.
	 * @param cause   the cause.
	 * @param message the detail message.
	 * @param args    additional arguments to be replaced in the message.
	 */
	public RHSException(String code, Throwable cause, String message, Object... args) {
		super(message = String.format(message, args), cause);
		exceptionCode = code;
		exceptionMessage = message;
	}

	/**
	 * Returns the exception code.
	 *
	 * @return A string representing the exception code.
	 */
	public String getCode() {
		return exceptionCode;
	}

	/**
	 * Returns the detail message.
	 *
	 * @return A string representing the detail message.
	 */
	public String getMessage() {
		return exceptionMessage;
	}

	/**
	 * Returns the description of the exception (containing the stack trace).
	 *
	 * @return A string containing the description.
	 */
	public String getDescription() {
		StringBuffer sb = new StringBuffer();
		PrintWriter pw = null;
		try {
			sb.append(String.format("%s(%s): \n", name, getCode()));
			StringWriter sw = new StringWriter();
			pw = new PrintWriter(sw);
			printStackTrace(pw);
			StringBuffer swsb = sw.getBuffer();
			if (swsb != null) {
				sb.append(swsb);
			}
		} catch (Exception e) {
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
		return sb.toString();
	}
}
