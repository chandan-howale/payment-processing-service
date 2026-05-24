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
public class CreatedStatusProcessor implements TransactionStatusProcessor {

	private final ModelMapper modelMapper;
	
	private final TransactionDao transactionDao;
	
	@Override
	public TransactionDto processStatus(TransactionDto txnDto) {
		log.info("Processing 'Created' status with txnDto: {}", txnDto);
		
		TransactionEntity txnEntity = modelMapper.map(txnDto, TransactionEntity.class);
		log.info("Mapped TransactionDto to TransactionEntity: {}", txnEntity);

		TransactionEntity responseEntity = transactionDao.createTransaction(txnEntity);
		log.info("Created TransactionEntity in database. Response TransactionEntity: {}", responseEntity);
		
		txnDto.setId(responseEntity.getId()); // Ensure ID is populated after creation
		
		log.info("Updated TransactionDto with generated ID: {}", txnDto);
		return txnDto;
	}

}
