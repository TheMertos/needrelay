package com.yagci.needrelay.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Domain/API error with machine-readable code and HTTP status.
 */
@Getter
public class ApiException extends RuntimeException {

	private final String code;
	private final HttpStatus status;

	/**
	 * Creates an API exception.
	 *
	 * @param code machine-readable error code
	 * @param message human-readable detail
	 * @param status HTTP status
	 */
	public ApiException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}
}
