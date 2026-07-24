package com.chandan.payments.service.impl.statusprocessors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.entity.TransactionEntity;
import com.chandan.payments.dao.interfaces.TransactionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

/**
 * Tests that SuccessStatusProcessor maps the DTO to an entity,
 * calls {@link TransactionDao#updateTransaction}, and returns the DTO.
 */
@ExtendWith(MockitoExtension.class)
class SuccessStatusProcessorTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TransactionDao transactionDao;

    private SuccessStatusProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SuccessStatusProcessor(modelMapper, transactionDao);
    }

    @Test
    void processStatus_updatesEntityAndReturnsDto() {
        TransactionDto dto = new TransactionDto();
        dto.setTxnReference("success-123");
        TransactionEntity entity = new TransactionEntity();

        when(modelMapper.map(dto, TransactionEntity.class)).thenReturn(entity);

        TransactionDto result = processor.processStatus(dto);
        assertSame(dto, result);
        verify(modelMapper).map(dto, TransactionEntity.class);
        verify(transactionDao).updateTransaction(entity);
    }
}