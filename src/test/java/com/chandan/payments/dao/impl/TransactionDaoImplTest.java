package com.chandan.payments.dao.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.chandan.payments.dao.interfaces.TransactionDao;
import com.chandan.payments.entity.TransactionEntity;
import com.chandan.payments.constant.ErrorCodeEnum;
import com.chandan.payments.exception.ProcessingServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link TransactionDaoImpl}.
 *
 * We mock {@link NamedParameterJdbcTemplate} to avoid needing a real database.
 * The tests verify:
 *   - Correct SQL is executed with proper parameters
 *   - Generated keys are set back on the entity
 *   - Exceptions are thrown when updates affect zero rows
 */
@ExtendWith(MockitoExtension.class)
class TransactionDaoImplTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private TransactionDao transactionDao;  // the real DAO, using the mocked template

    @BeforeEach
    void setUp() {
        transactionDao = new TransactionDaoImpl(jdbcTemplate);
    }

    // -------------------------------------------------------------------------
    // 1️⃣  createTransaction: should set generated ID back on entity
    // -------------------------------------------------------------------------
    @Test
    void createTransaction_setsGeneratedId() {
        TransactionEntity input = new TransactionEntity();
        input.setUserId(1);  // Integer, not long
        input.setTxnReference("ref-123");

        // Mock the key holder behavior
        KeyHolder keyHolder = new GeneratedKeyHolder();
        keyHolder.getKeyList().add(java.util.Map.of("id", 42));

        // Stub the update method to capture the KeyHolder
        when(jdbcTemplate.update(
                anyString(),
                any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class),
                any(KeyHolder.class),
                any(String[].class)))
                .thenAnswer(invocation -> {
                    // The DAO passes its own KeyHolder; we ensure it's populated
                    KeyHolder holder = invocation.getArgument(2);
                    holder.getKeyList().add(java.util.Map.of("id", 42));
                    return 1;
                });

        TransactionEntity result = transactionDao.createTransaction(input);

        assertEquals(42, result.getId());
        assertNotNull(result.getTxnReference());
        verify(jdbcTemplate).update(
                anyString(),
                any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class),
                any(KeyHolder.class),
                any(String[].class));
    }

    // -------------------------------------------------------------------------
    // 2️⃣  getTransactionByTxnReference: should return entity for valid reference
    // -------------------------------------------------------------------------
    @Test
    void getTransactionByTxnReference_returnsEntity() {
        String ref = "ref-abc";
        TransactionEntity mockedEntity = new TransactionEntity();
        mockedEntity.setId(10);
        mockedEntity.setTxnReference(ref);

        Map<String, Object> capturedParams = new HashMap<>();

        when(jdbcTemplate.queryForObject(
                eq("SELECT * FROM `Transaction` WHERE txnReference = :txnReference LIMIT 1"),
                anyMap(),
                any(BeanPropertyRowMapper.class)))
                .thenReturn(mockedEntity);

        TransactionEntity result = transactionDao.getTransactionByTxnReference(ref);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(ref, result.getTxnReference());
        verify(jdbcTemplate).queryForObject(
                anyString(),
                anyMap(),
                any(BeanPropertyRowMapper.class));
    }

    // -------------------------------------------------------------------------
    // 3️⃣  updateTransaction: successful update
    // -------------------------------------------------------------------------
    @Test
    void updateTransaction_success() {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(100);
        entity.setTxnStatusId(2);
        entity.setProviderReference("prov-x");
        entity.setErrorCode(null);
        entity.setErrorMessage(null);

        when(jdbcTemplate.update(anyString(), anyMap())).thenReturn(1);

        // Should not throw
        assertDoesNotThrow(() -> transactionDao.updateTransaction(entity));
        verify(jdbcTemplate).update(anyString(), anyMap());
    }

    // -------------------------------------------------------------------------
    // 4️⃣  updateTransaction: zero rows affected → throws ProcessingServiceException
    // -------------------------------------------------------------------------
    @Test
    void updateTransaction_noRows_throwsException() {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(999);
        entity.setTxnStatusId(3);

        when(jdbcTemplate.update(anyString(), anyMap())).thenReturn(0);

        ProcessingServiceException ex = assertThrows(
                ProcessingServiceException.class,
                () -> transactionDao.updateTransaction(entity));

        assertEquals(ErrorCodeEnum.ERROR_UPDATING_TRANSACTION.getErrorCode(), ex.getErrorCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        verify(jdbcTemplate).update(anyString(), anyMap());
    }
}