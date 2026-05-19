package com.chandan.payments.service.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chandan.payments.http.HttpRequest;
import com.chandan.payments.http.HttpServiceEngine;
import com.chandan.payments.pojo.CreatePaymentRequest;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.service.helper.PPCreateOrderHelper;
import com.chandan.payments.service.interfaces.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
	
	private final PPCreateOrderHelper ppCreateOrderHelper;
	
	private final HttpServiceEngine httpServiceEngine;

	@Override
	public String createPayment(CreatePaymentRequest createPaymentRequest) {
		log.info("Creating payment in PaymentServiceImpl... || createPaymentRequest: {} ", createPaymentRequest);
		
		return "Payment created successfully! in service " + createPaymentRequest;
	}

	@Override
	public String initiatePayment(String txnReference, InitiatePaymentRequest initiatePaymentRequest) {
		log.info("Initiating payment in PaymentServiceImpl... txnReference:{} "
				+ "| initiatePaymentRequest:{}", txnReference, initiatePaymentRequest);
		
		//Make an API call to paypal-provider-service createOrder endpoint
		/*
		 * 1. prepare HttpRequest for createOrder - DONE
		 * 2. pass to HttpServiceEngine
		 * 3. handle response
		 */
		
		HttpRequest httpReq = ppCreateOrderHelper.prepareHttpRequest(txnReference, initiatePaymentRequest);
		log.info("Prepared HttpRequest for PayPalProvider Create Order API.. httpReq:{}", httpReq);
		
		ResponseEntity<String> httpResponse = httpServiceEngine.makeHttpCall(httpReq);
		log.info("Received httpResponse from PayPalProvider Create Order API.. httpResponse:{}", httpResponse);
		
		return "Payment initiated successfully in service!"
				+ " - " + txnReference
				+ " | " + initiatePaymentRequest
				+ " | " + httpResponse.getBody();
	}

	@Override
	public String capturePayment(String txnReference) {
		log.info("Capturing payment in PaymentServiceImpl...txnReference:{}", txnReference);
		
		return "Payment captured successfully! for txnReference: " + txnReference + " in service";
	}

}
