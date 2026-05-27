package com.chandan.payments.service.impl.statusprocessors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.chandan.payments.dao.interfaces.TransactionDao;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.entity.TransactionEntity;
import com.chandan.payments.service.interfaces.TransactionStatusProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuccessStatusProcessor implements TransactionStatusProcessor {
	
	private final ModelMapper modelMapper;

	private final TransactionDao transactionDao;

	@Override
	public TransactionDto processStatus(TransactionDto txnDto) {
		log.info("Processing 'Success' status with txnDto: {}", txnDto);
		
		//convert DTO to Entity
		TransactionEntity txnEntity = modelMapper.map(txnDto, TransactionEntity.class);
		
		transactionDao.updateTransaction(txnEntity);
		log.info("Updated transaction in DB for 'Success' status. txnEntity: {}", txnEntity);

		return txnDto;
	}

}
