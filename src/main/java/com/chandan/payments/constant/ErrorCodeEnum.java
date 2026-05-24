package com.chandan.payments.constant;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
	
	// Define your ENum constants here
	GENERIC_ERROR("20000", "Something went wrong. Please try again later."),
	RESOURCE_NOT_FOUND("20001", "Invalid URL. Please check and try again."),
	PAYPAL_PROVIDER_SERVICE_UNAVAILABLE("20002", "PayPal-provider service is currently unavailable. Please try again later."),
	STATUS_NOT_FOUND("20003", "Payment status not found."),
	PAYPAL_PROVIDER_UNKNOWN_ERROR("20004", "An unknown error occurred in the paypal-provider service. Please try again later."),
	ERROR_UPDATING_TRANSACTION("20005", "Error updating transaction details.");
	
	

    private final String errorCode;
    private final String errorMessage;
    
    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
