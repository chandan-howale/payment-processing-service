package com.chandan.payments.dao.interfaces;

import com.chandan.payments.entity.TransactionEntity;

public interface TransactionDao {
	
	public TransactionEntity createTransaction(TransactionEntity txnEntity);
	public TransactionEntity getTransactionByTxnReference(String txnReference);
	
	public void updateTransaction(TransactionEntity txnEntity);
}
