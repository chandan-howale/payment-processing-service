package com.chandan.payments.service.impl;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chandan.payments.dao.interfaces.TransactionDao;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.entity.TransactionEntity;
import com.chandan.payments.exception.ProcessingServiceException;
import com.chandan.payments.http.HttpRequest;
import com.chandan.payments.http.HttpServiceEngine;
import com.chandan.payments.paypalprovider.PPOrderResponse;
import com.chandan.payments.pojo.CreatePaymentRequest;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.pojo.PaymentResponse;
import com.chandan.payments.service.PaymentStatusService;
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
	
	private final PaymentStatusService paymentStatusService;
	
	private final ModelMapper modelMapper;
	
	private final TransactionDao transactionDao;

	@Override
	public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
		log.info("Creating payment in PaymentServiceImpl... || createPaymentRequest: {} ", createPaymentRequest);
		
		TransactionDto txnDto = modelMapper.map(createPaymentRequest, TransactionDto.class);
		log.info("Mapped CreatePaymentRequest to TransactionDto... dto: {} ", txnDto);
		
		int txnStatusId = 1; //'Created' status
		String txnReference = generateUniqueTxnReference();
		
		txnDto.setTxnStatusId(txnStatusId);
		txnDto.setTxnReference(txnReference);
		
		TransactionDto response = paymentStatusService.processPayment(txnDto);
		log.info("Response from TransactionStatusProcessor: {} ", response);
		
		PaymentResponse paymentResponse = new PaymentResponse();
		paymentResponse.setTxnReference(response.getTxnReference());
		paymentResponse.setTxnStatusId(response.getTxnStatusId());
		log.info("Prepared PaymentResponse: {} ", paymentResponse);
		
		return paymentResponse;
	}

	private String generateUniqueTxnReference() {
		return UUID.randomUUID().toString();
	}

	@Override
	public PaymentResponse initiatePayment(String txnReference, InitiatePaymentRequest initiatePaymentRequest) {
		log.info("Initiating payment in PaymentServiceImpl... txnReference:{} "
				+ "| initiatePaymentRequest:{}", txnReference, initiatePaymentRequest);
		
		TransactionEntity txnEntity = transactionDao.getTransactionByTxnReference(txnReference);
		log.info("Fetched TransactionEntity from DB for txnReference: {}... txnEntity: {}", txnReference, txnEntity);
		
		//use modelmapper to convert TransactionEntity to TransactionDto
		TransactionDto txnDto = modelMapper.map(txnEntity, TransactionDto.class);
		log.info("Mapped TransactionEntity to TransactionDto... txnDto: {}", txnDto);
		
		//update txn status to 'Initiated' (statusId = 2)
		txnDto.setTxnStatusId(2); //'Initiated' status
		TransactionDto response = paymentStatusService.processPayment(txnDto);
		log.info("Response from TransactionStatusProcessor after updating status to 'Initiated': {} ", response);

		
		//Make an API call to paypal-provider-service createOrder endpoint
		/*
		 * 1. prepare HttpRequest for createOrder - DONE
		 * 2. pass to HttpServiceEngine - DONE
		 * 3. handle response - DONE
		 */
		
		HttpRequest httpReq = ppCreateOrderHelper.prepareHttpRequest(txnReference, initiatePaymentRequest, txnDto);
		log.info("Prepared HttpRequest for PayPalProvider Create Order API.. httpReq:{}", httpReq);
		
		PPOrderResponse ppOrderResponse = null;
		try {
			
			ResponseEntity<String> httpResponse = httpServiceEngine.makeHttpCall(httpReq);
			log.info("Received httpResponse from PayPalProvider Create Order API.. httpResponse:{}", httpResponse);
			
			ppOrderResponse = ppCreateOrderHelper.processResponse(httpResponse);
			log.info("Processed PayPalProvider Create Order response.. ppOrderResponse: {}", ppOrderResponse);
			
		} catch (ProcessingServiceException e) {
			
			txnDto.setTxnStatusId(6); //'Failed' status
			txnDto.setErrorCode(e.getErrorCode());
			txnDto.setErrorMessage(e.getErrorMessage());
			
			paymentStatusService.processPayment(txnDto);
			log.info("Updated transaction status to FAILED for txnReference: {}", txnReference);
			
			throw e;		//rethrow the exception to be handled by global exception handler 
						//and return appropriate response to client
		}
		
		//update txn to PENDING status (statusId = 3)
		txnDto.setTxnStatusId(3); //'Pending' status
		txnDto.setProviderReference(ppOrderResponse.getOrderId());
		response = paymentStatusService.processPayment(txnDto);
	
		
		PaymentResponse paymentResponse = new PaymentResponse();
		paymentResponse.setTxnReference(txnDto.getTxnReference());
		paymentResponse.setTxnStatusId(txnDto.getTxnStatusId());
		paymentResponse.setProviderReference(ppOrderResponse.getOrderId());
		paymentResponse.setRedirectUrl(ppOrderResponse.getRedirectUrl());
		log.info("Prepared PaymentResponse to return from initiatePayment: {} ", paymentResponse);
		
		
		return paymentResponse;
	}

	@Override
	public String capturePayment(String txnReference) {
		log.info("Capturing payment in PaymentServiceImpl...txnReference:{}", txnReference);
		
		return "Payment captured successfully! for txnReference: " + txnReference + " in service";
	}

}
