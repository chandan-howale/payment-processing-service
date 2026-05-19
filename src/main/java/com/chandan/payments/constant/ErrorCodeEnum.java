package com.chandan.payments.constant;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
	
	// Define your ENum constants here
	GENERIC_ERROR("20000", "Something went wrong. Please try again later."),
	RESOURCE_NOT_FOUND("20001", "Invalid URL. Please check and try again."),
	PAYPAL_PROVIDER_SERVICE_UNAVAILABLE("20002", "PayPal-provider service is currently unavailable. Please try again later.");
	
	

    private final String errorCode;
    private final String errorMessage;
    
    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
