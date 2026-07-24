package com.chandan.payments.service.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chandan.payments.constant.ErrorCodeEnum;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.exception.ProcessingServiceException;
import com.chandan.payments.http.HttpRequest;
import com.chandan.payments.paypalprovider.PPCreateOrderReq;
import com.chandan.payments.paypalprovider.PPErrorResponse;
import com.chandan.payments.paypalprovider.PPOrderResponse;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PPCreateOrderHelper {
	
	private final JsonUtil jsonUtil;
	
	@Value("${paypalprovider.create.order.url}")
	private String paypalProviderCreateOrderUrl;


	public HttpRequest prepareHttpRequest(
			String txnReference, InitiatePaymentRequest initiatePaymentRequest, TransactionDto txnDto) {
		
		log.info("Preparing HttpRequest for PayPal Create Order API.. "
				+ "|| txnReference:{} | initiatePaymentRequest:{} | txnDto:{}",
				txnReference, initiatePaymentRequest, txnDto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		PPCreateOrderReq ppCreateOrderReq = new PPCreateOrderReq();
		ppCreateOrderReq.setCurrencyCode(txnDto.getCurrency());
		ppCreateOrderReq.setAmount(txnDto.getAmount().doubleValue());
		ppCreateOrderReq.setReturnUrl(initiatePaymentRequest.getSuccessUrl());
		ppCreateOrderReq.setCancelUrl(initiatePaymentRequest.getCancelUrl());
		
		String requestAsJson = jsonUtil.toJson(ppCreateOrderReq);
		
		// create HttpRequest object
		HttpRequest httpRequest = new HttpRequest();
		httpRequest.setHttpMethod(HttpMethod.POST);
		httpRequest.setUrl(paypalProviderCreateOrderUrl);
		httpRequest.setHttpHeaders(headers);
		httpRequest.setBody(requestAsJson );

		return httpRequest;
	}


	public PPOrderResponse processResponse(ResponseEntity<String> httpResponse) {
		log.info("Processing from PayPal Create Order response...|| httpResponse: {}", httpResponse);
		
		if (httpResponse.getStatusCode().equals(HttpStatus.OK)) {
			log.info("PayPal Create Order API call successful. Response body: {}", httpResponse.getBody());
			
			PPOrderResponse responseObj = jsonUtil.fromJson(httpResponse.getBody(), PPOrderResponse.class);
			
			if (responseObj != null
					&& responseObj.getOrderId() != null
					&& responseObj.getPaypalStatus() != null
					&& responseObj.getRedirectUrl() != null)
			{
				log.info("Parsed PayPal order response successfully: {}", responseObj);
				return responseObj;
			} else {
				log.error("Failed to parse PayPal order response..");
			}
		}
		
		if(httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError()) {
			log.error("Received error response from PayPal Create Order API.. httpResponse: {}", httpResponse);
			
			PPErrorResponse errorResponse = jsonUtil.fromJson(httpResponse.getBody(), PPErrorResponse.class);
			
			throw new ProcessingServiceException( 
					errorResponse.getErrorCode(),
					errorResponse.getErrorMessage(),
					HttpStatus.valueOf(httpResponse.getStatusCode().value()));
		}
		
		log.error("Received unexpected response from PayPal Create Order API.. httpResponse: {}", httpResponse);
		
		throw new ProcessingServiceException(
				ErrorCodeEnum.PAYPAL_PROVIDER_UNKNOWN_ERROR.getErrorCode(),
				ErrorCodeEnum.PAYPAL_PROVIDER_UNKNOWN_ERROR.getErrorMessage(),
				HttpStatus.BAD_GATEWAY);
	}
}
