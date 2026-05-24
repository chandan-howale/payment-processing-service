package com.chandan.payments.dao.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.chandan.payments.constant.ErrorCodeEnum;
import com.chandan.payments.dao.interfaces.TransactionDao;
import com.chandan.payments.entity.TransactionEntity;
import com.chandan.payments.exception.ProcessingServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class TransactionDaoImpl implements TransactionDao {
	
	private final NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public TransactionEntity createTransaction(TransactionEntity txnEntity) {
		log.info("Creating TransactionEntityin DB... txnEntity: {}", txnEntity);
		
		String sql = """
				    INSERT INTO payments.`Transaction`
				    (
				        userId,
				        paymentMethodId,
				        providerId,
				        paymentTypeId,
				        txnStatusId,
				        amount,
				        currency,
				        merchantTransactionReference,
				        txnReference,
				        providerReference,
				        errorCode,
				        errorMessage,
				        retryCount
				    )
				    VALUES
				    (
				        :userId,
				        :paymentMethodId,
				        :providerId,
				        :paymentTypeId,
				        :txnStatusId,
				        :amount,
				        :currency,
				        :merchantTransactionReference,
				        :txnReference,
				        :providerReference,
				        :errorCode,
				        :errorMessage,
				        :retryCount
				    )
				""";
		
		SqlParameterSource params = new BeanPropertySqlParameterSource(txnEntity);
		
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(sql, params, keyHolder, new String[] { "id" });

		//extract generated id and set it back to the entity
		Number generatedId = keyHolder.getKey();
		if (generatedId != null) {
			txnEntity.setId(generatedId.intValue());
		}

		log.info("Transaction created with ID: {}", txnEntity.getId());
		return txnEntity;
	}

	@Override
	public TransactionEntity getTransactionByTxnReference(String txnReference) {

		String sql = "SELECT * FROM `Transaction` WHERE txnReference = :txnReference LIMIT 1";

		Map<String, Object> params = new HashMap<>();
		params.put("txnReference", txnReference);


		TransactionEntity txnEntity = jdbcTemplate.queryForObject(
				sql,
				params,
				new BeanPropertyRowMapper<>(TransactionEntity.class)
				);
		
		log.info("Fetched TransactionEntity for txnReference {}: {}", txnReference, txnEntity);
		return txnEntity;
	}

	@Override
	public void updateTransaction(TransactionEntity txnEntity) {
		
		String sql = """
				    UPDATE `Transaction`
				    SET txnStatusId = :txnStatusId,
				        providerReference = :providerReference,
				        errorCode = :errorCode,
				        errorMessage = :errorMessage
				    WHERE id = :id
				""";

		Map<String, Object> params = new HashMap<>();
		params.put("txnStatusId", txnEntity.getTxnStatusId());
		params.put("providerReference", txnEntity.getProviderReference());
		params.put("errorCode", txnEntity.getErrorCode());
		params.put("errorMessage", txnEntity.getErrorMessage());
		params.put("id", txnEntity.getId());

		int rowsAffected = jdbcTemplate.update(sql, params);
		log.info("Updated TransactionEntity with ID {}. Rows affected: {}", txnEntity.getId(), rowsAffected);

		if (rowsAffected == 0) {
			log.error("No transaction found with ID {} to update.", txnEntity.getId());
			throw new ProcessingServiceException(
					ErrorCodeEnum.ERROR_UPDATING_TRANSACTION.getErrorCode(), 
					ErrorCodeEnum.ERROR_UPDATING_TRANSACTION.getErrorMessage(), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
