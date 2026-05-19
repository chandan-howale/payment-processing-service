package com.chandan.payments.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.chandan.payments.constant.ErrorCodeEnum;
import com.chandan.payments.exception.ProcessingServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpServiceEngine {
	
	private final RestClient restClient;

	public ResponseEntity<String> makeHttpCall(HttpRequest httpRequest) {
		log.info("Making HTTP call in HttpServiceEngine");
		

		// Making the actual HTTP call --->
		try {
			ResponseEntity<String> httpResponse = restClient
					.method(httpRequest.getHttpMethod())
					.uri(httpRequest.getUrl())
					.headers(
							restClientHeaders -> 
							restClientHeaders.addAll(
									httpRequest.getHttpHeaders()))
					.body(httpRequest.getBody())
					.retrieve()
					.toEntity(String.class);
			
			log.info("HTTP call completed in HttpServiceEngine with httpResponse: {}", httpResponse);
			
			return httpResponse;
			
		
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			log.error("HTTP error occurred while making HTTP call in HttpServiceEngine: {}", e.getMessage(), e);
			
			//if the error is gateway timeout or service unavailable from PayPal, throw PaypalProviderException with SERVICE_UNAVAILABLE
			if (e.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT || 
					e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
				log.error("Service is unavailable or timed out!!");
				
				throw new ProcessingServiceException(
						ErrorCodeEnum.PAYPAL_PROVIDER_SERVICE_UNAVAILABLE.getErrorCode(),
						ErrorCodeEnum.PAYPAL_PROVIDER_SERVICE_UNAVAILABLE.getErrorMessage(),
						HttpStatus.SERVICE_UNAVAILABLE);
			}
			
			String errorResponseBody = e.getResponseBodyAsString();
			log.info("Error response body: {}", errorResponseBody);
			
			return ResponseEntity
					.status(e.getStatusCode())
					.body(errorResponseBody);
			
		} catch (Exception e) {
		
		log.error("Exception occurred while making HTTP call in HttpServiceEngine: {}", e.getMessage(), e);
		
		throw new ProcessingServiceException(
				ErrorCodeEnum.PAYPAL_PROVIDER_SERVICE_UNAVAILABLE.getErrorCode(),
				ErrorCodeEnum.PAYPAL_PROVIDER_SERVICE_UNAVAILABLE.getErrorMessage(),
				HttpStatus.SERVICE_UNAVAILABLE);
	}
		
	}

}
