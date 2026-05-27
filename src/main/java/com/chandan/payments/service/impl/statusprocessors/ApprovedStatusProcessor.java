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
public class ApprovedStatusProcessor implements TransactionStatusProcessor {
	
	private final TransactionDao transactionDao;
	
	private final ModelMapper modelMapper;

	@Override
	public TransactionDto processStatus(TransactionDto txnDto) {
		log.info("Processing 'Approved' status with txnDto: {}", txnDto);
		
		//convert DTO to Entity
		TransactionEntity txnEntity = modelMapper.map(txnDto, TransactionEntity.class);
		log.info("In ApprovedStatusProcessor | Mapped TransactionDto to TransactionEntity: {}",
				txnEntity);
		
		transactionDao.updateTransaction(txnEntity);
		log.info("Updated transaction in DB for 'Approved' status. txnEntity: {}", txnEntity);

		return txnDto;
	}

}
