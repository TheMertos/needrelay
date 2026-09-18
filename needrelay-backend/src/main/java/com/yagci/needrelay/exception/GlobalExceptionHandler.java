package com.yagci.needrelay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Maps exceptions to RFC 7807 ProblemDetail responses with a {@code code} property.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles application {@link ApiException}s.
	 *
	 * @param ex thrown API exception
	 * @return problem detail with code
	 */
	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApiException(ApiException ex) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
		detail.setTitle(ex.getStatus().getReasonPhrase());
		detail.setProperty("code", ex.getCode());
		return detail;
	}

	/**
	 * Handles Bean Validation failures on request bodies.
	 *
	 * @param ex validation exception
	 * @return problem detail with field messages
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.collect(Collectors.joining("; "));
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
		detail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
		detail.setProperty("code", "VALIDATION_ERROR");
		return detail;
	}

	/**
	 * Formats a single field error for the problem detail message.
	 *
	 * @param error field error
	 * @return "field: message"
	 */
	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}
}
