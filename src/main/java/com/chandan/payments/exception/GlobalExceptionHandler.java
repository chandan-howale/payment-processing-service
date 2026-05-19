package com.chandan.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.chandan.payments.constant.ErrorCodeEnum;
import com.chandan.payments.pojo.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ProcessingServiceException.class)
	public ResponseEntity<ErrorResponse> handlePaypalProviderException(ProcessingServiceException ex) {
		log.error("Handling PaypalProviderException: {}", ex.getErrorMessage(), ex);

		ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), ex.getErrorMessage());

		return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
	}

	// NoResourceFoundException
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
			NoResourceFoundException ex) {
		log.error("Handling NoResourceFoundException: {}", ex.getMessage(), ex);

		ErrorResponse error = new ErrorResponse(
				ErrorCodeEnum.RESOURCE_NOT_FOUND.getErrorCode(), 
				ErrorCodeEnum.RESOURCE_NOT_FOUND.getErrorMessage());

		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND); 
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex) {
		log.error("Handling generic exception: {}", ex.getMessage(), ex);

		ErrorResponse errorResponse = new ErrorResponse(
				ErrorCodeEnum.GENERIC_ERROR.getErrorCode(),
				ErrorCodeEnum.GENERIC_ERROR.getErrorMessage());

		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
