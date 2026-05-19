package com.chandan.payments.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ProcessingServiceException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3775101319528756949L;
	private final String errorCode;
	private final String errorMessage;
	
	private final HttpStatus httpStatus;

	public ProcessingServiceException(String errorCode, String errorMessage, HttpStatus httpStatus) {
		super(errorMessage); // Pass message to superclass for logging purposes
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.httpStatus = httpStatus;
	}

}
