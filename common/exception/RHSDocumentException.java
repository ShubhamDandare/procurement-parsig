package com.kpmg.rcm.sourcing.common.exception;

/**
 * Thrown to indicate that an operation for a single document has not properly
 * been executed.
 */
public class RHSDocumentException extends RHSException {

	static String name = "RHSDocumentException";

	/**
	 * Constructs a new <code>RHSDocumentException</code> with the specified
	 * exception code and detail message.
	 *
	 * @param code    the exception code.
	 * @param message the detail message.
	 * @param args    additional arguments to be replaced in the message.
	 */
	public RHSDocumentException(String code, String message, Object... args) {
		super(code, message, args);
	}

	/**
	 * Constructs a new <code>RHSDocumentException</code> with the specified detail
	 * message and cause.
	 *
	 * @param code  the exception code.
	 * @param cause the cause.
	 */
	public RHSDocumentException(String code, Throwable cause) {
		super(code, cause);
	}

	/**
	 * Constructs a new <code>RHSDocumentException</code> with the specified
	 * exception code, cause and detail message.
	 *
	 * @param code    the exception code.
	 * @param cause   the cause.
	 * @param message the detail message.
	 * @param args    additional arguments to be replaced in the message.
	 */
	public RHSDocumentException(String code, Throwable cause, String message, Object... args) {
		super(code, cause, message, args);
	}
}
