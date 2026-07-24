package com.chandan.payments.service.interfaces;

import com.chandan.payments.dto.TransactionDto;

public interface TransactionStatusProcessor {
	
	public TransactionDto  processStatus(TransactionDto txnDto);

}
