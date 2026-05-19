package com.chandan.payments.service.helper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.chandan.payments.http.HttpRequest;
import com.chandan.payments.paypalprovider.PPCreateOrderReq;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PPCreateOrderHelper {
	
	private final JsonUtil jsonUtil;

	public HttpRequest prepareHttpRequest(
			String txnReference, InitiatePaymentRequest initiatePaymentRequest) {
		
		log.info("Preparing HttpRequest for PayPal Create Order API.. "
				+ "|| txnReference:{} | initiatePaymentRequest:{}",
				txnReference, initiatePaymentRequest);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		PPCreateOrderReq ppCreateOrderReq = new PPCreateOrderReq();
		ppCreateOrderReq.setCurrencyCode("USD");
		ppCreateOrderReq.setAmount(1.5);
		ppCreateOrderReq.setReturnUrl(initiatePaymentRequest.getSuccessUrl());
		ppCreateOrderReq.setCancelUrl(initiatePaymentRequest.getCancelUrl());
		
		String requestAsJson = jsonUtil.toJson(ppCreateOrderReq);
		
		// create HttpRequest object
		HttpRequest httpRequest = new HttpRequest();
		httpRequest.setHttpMethod(HttpMethod.POST);
		httpRequest.setUrl("http://localhost:8083/payments");
		httpRequest.setHttpHeaders(headers);
		httpRequest.setBody(requestAsJson );

		return httpRequest;
	}

}
