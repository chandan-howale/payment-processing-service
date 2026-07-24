package com.chandan.payments.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.chandan.payments.constant.ErrorCodeEnum;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.exception.ProcessingServiceException;
import com.chandan.payments.service.interfaces.TransactionStatusProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentStatusService {
	
	private final PaymentStatusFactory paymentStatusFactory;
	
	public TransactionDto processPayment(TransactionDto txnDto) {
		log.info("Processing payment status in PaymentStatusService... txnDto: {}", txnDto);
		
		int statusId = txnDto.getTxnStatusId();
		
		TransactionStatusProcessor processor = paymentStatusFactory.getStatusProcessor(statusId);
		log.info("Obtained TransactionStatusProcessor: {}", processor);
		
		if (processor == null) {
			log.error("No processor found for statusId: {}", statusId);
			
			throw new ProcessingServiceException(
					ErrorCodeEnum.STATUS_NOT_FOUND.getErrorCode(),
					ErrorCodeEnum.STATUS_NOT_FOUND.getErrorMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		TransactionDto response = processor.processStatus(txnDto);
		log.info("Processed payment status. Response: {}", response);
		
		return response;
		
	}
}
