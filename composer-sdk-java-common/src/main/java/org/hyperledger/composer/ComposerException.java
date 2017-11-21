/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer;

public class ComposerException extends Exception {
	private static final long serialVersionUID = -7645116399152476663L;

	public static final int NOT_AUTHORIZED = 401;
	public static final int NOT_EXIST_CODE = 404;
	public static final int INTERNAL_ERROR_CODE = 500;
	public static final int SERVER_ERROR = 1001;
	public static final int APPLICATION_ERROR = 1002;
	public static final int CREATE_FAILURE_CODE = 1003;
	public static final int PARSE_ERROR_CODE = 1004;
	public static final int CAN_NOT_UPDATE_CODE = 1006;
	public static final int INVALID_REQUEST_CODE = 1007;
	public static final int NO_IMPLEMENTATION_CODE = 1008;
	public static final int ALREADY_EXIST_CODE = 1009;
	public static final int SERVER_NOT_FOUND_CODE = 1010;
	public static final int TIMEOUT_ERROR = 1011;
	public static final int FABRIC_SDK_ERROR = 1012;
	public static final int NULL_FIELD_ERROR = 1013;
	public static final int INVALID_INPUT_ERROR = 1014;
	public static final int ERROR_FIELD_TYPE = 1016;
	public static final int ERROR_MESSAGE_TYPE = 1019;

	private int errorCode;

	private String message;

	public ComposerException(String message) {
		this(INTERNAL_ERROR_CODE, message);
	}

	public int getErrorCode() {
		return errorCode;
	}

	public ComposerException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.message = message;
		updateErrorCodeByMessage(message);
	}

	public ComposerException(Throwable cause) {
		this(INTERNAL_ERROR_CODE, null, cause);
	}

	public ComposerException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.message = message;
		this.errorCode = errorCode;

		if (this.errorCode == INTERNAL_ERROR_CODE && cause != null) {
			Throwable rootCause = getRootCause(cause);
			String rootMessage = rootCause.getMessage();
			if (rootMessage == null || rootMessage.isEmpty()) {
				rootMessage = rootCause.getClass().getName();
			}
			if (rootCause instanceof ComposerException) {
				this.errorCode = ((ComposerException) rootCause).errorCode;
			} else {
				updateErrorCodeByMessage(rootMessage);
			}

			if (message == null || message.isEmpty()) {
				this.message = rootMessage;
			}
		}
	}

	private void updateErrorCodeByMessage(String message) {
		if (message.contains("not exist")) {
			this.errorCode = NOT_EXIST_CODE;
		} else if (message.contains("already exist")) {
			this.errorCode = ALREADY_EXIST_CODE;
		}
	}

	private Throwable getRootCause(Throwable cause) {
		if (cause.getCause() == null || cause.equals(cause.getCause())) {
			return cause;
		}
		return getRootCause(cause.getCause());
	}

	public String getRootMessage() {
		return getRootCause(this).getMessage();
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ComposerException that = (ComposerException) o;

		return errorCode == that.errorCode && (message != null ? message.equals(that.message) : that.message == null);
	}

	@Override
	public int hashCode() {
		int result = errorCode;
		result = 31 * result + (message != null ? message.hashCode() : 0);
		return result;
	}
}
