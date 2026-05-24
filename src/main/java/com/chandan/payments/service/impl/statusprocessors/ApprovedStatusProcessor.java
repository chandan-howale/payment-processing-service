package com.chandan.payments.service.impl.statusprocessors;

import org.springframework.stereotype.Service;

import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.service.interfaces.TransactionStatusProcessor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApprovedStatusProcessor implements TransactionStatusProcessor {

	@Override
	public TransactionDto processStatus(TransactionDto txnDto) {
		log.info("Processing 'Approved' status with txnDto: {}", txnDto);

		return txnDto;
	}

}
