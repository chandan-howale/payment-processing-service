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
import com.chandan.payments.paypalprovider.PPErrorResponse;
import com.chandan.payments.paypalprovider.PPOrderResponse;
import com.chandan.payments.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PPCaptureOrderHelper {
	
	private final JsonUtil jsonUtil;
	
	private String PROVIDER_REF = "{provider-reference}";
	
	@Value("${paypalprovider.capture.order.url}")
	private String paypalProviderCaptureOrderUrlTemplate;

	public HttpRequest prepareHttpRequest(String txnReference, TransactionDto txnDto) {
		log.info("Preparing HttpRequest for PayPal Capture Order API.. "
				+ "of txnReference:{} | txnDto:{}",
				txnReference, txnDto);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		String paypalProviderCaptureOrderUrl = paypalProviderCaptureOrderUrlTemplate.replace(
				PROVIDER_REF, txnDto.getProviderReference());
		log.info("Prepared PayPal Provider Capture Order URL: {}", paypalProviderCaptureOrderUrl);
		
		// create HttpRequest object
		HttpRequest httpRequest = new HttpRequest();
		httpRequest.setHttpMethod(HttpMethod.POST);
		httpRequest.setUrl(paypalProviderCaptureOrderUrl);
		httpRequest.setHttpHeaders(headers);
		httpRequest.setBody("");
		
		log.info("Prepared HttpRequest for PayPal Capture Order API: {}", httpRequest);
		
		return httpRequest;
	}
	
	public PPOrderResponse processResponse(ResponseEntity<String> httpResponse) {
		log.info("Processing response from PayPal Capture Order API.. httpResponse: {}", httpResponse);
		
		if (httpResponse.getStatusCode().equals(HttpStatus.OK)) {
			log.info("PayPal Capture Order API call successful. Response body: {}", httpResponse.getBody());

			PPOrderResponse responseObj = jsonUtil.fromJson(
					httpResponse.getBody(), PPOrderResponse.class);

			if (responseObj != null
					&& responseObj.getOrderId() != null
					&& responseObj.getPaypalStatus() != null
					&& responseObj.getPaypalStatus().equals("COMPLETED"))
			{
				log.info("Parsed PayPal Capture Order response successfully: {}", responseObj);
				
				return responseObj;
			} else {
				log.error("Failed to parse PayPal Capture Order response or order not completed..");
			}
		}
		
		//handle 4xx and 5xx
		if (httpResponse.getStatusCode().is4xxClientError() 
				|| httpResponse.getStatusCode().is5xxServerError()) { 
			
			log.error("Received 4xx, 5xx error response from PayPalProvider service");
			
			PPErrorResponse errorResponse = jsonUtil.fromJson( 
					httpResponse.getBody(), PPErrorResponse.class);
			
			throw new ProcessingServiceException(
					errorResponse.getErrorCode(),
					errorResponse.getErrorMessage(),
					HttpStatus.valueOf(
							httpResponse.getStatusCode().value()));
			
		}

		log.error("Unexpected response from PayPal Capture Order API."
				+ "httpResponse: {}", httpResponse);
		
		throw new ProcessingServiceException(
				ErrorCodeEnum.PAYPAL_PROVIDER_SERVICE_UNAVAILABLE.getErrorCode(),
				ErrorCodeEnum.PAYPAL_PROVIDER_SERVICE_UNAVAILABLE.getErrorMessage(),
				HttpStatus.SERVICE_UNAVAILABLE);
	}
}
