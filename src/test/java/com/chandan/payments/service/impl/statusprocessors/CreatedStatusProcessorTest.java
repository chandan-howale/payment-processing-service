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
 * Unit tests for {@link CreatedStatusProcessor}.
 *
 * The processor should:
 *   1. Map the incoming {@link TransactionDto} to a {@link TransactionEntity} using {@link ModelMapper}.
 *   2. Call {@link TransactionDao#createTransaction} with the mapped entity.
 *   3. Populate the generated ID back into the DTO.
 */
@ExtendWith(MockitoExtension.class)
class CreatedStatusProcessorTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TransactionDao transactionDao;

    private CreatedStatusProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CreatedStatusProcessor(modelMapper, transactionDao);
    }

    @Test
    void processStatus_createsEntityAndSetsId() {
        // Arrange
        TransactionDto inputDto = new TransactionDto();
        inputDto.setTxnReference("ref-abc");
        TransactionEntity mappedEntity = new TransactionEntity();
        TransactionEntity persistedEntity = new TransactionEntity();
        persistedEntity.setId(42); // simulated generated ID

        when(modelMapper.map(inputDto, TransactionEntity.class)).thenReturn(mappedEntity);
        when(transactionDao.createTransaction(mappedEntity)).thenReturn(persistedEntity);

        // Act
        TransactionDto result = processor.processStatus(inputDto);

        // Assert
        assertEquals(42, result.getId());
        verify(modelMapper, times(1)).map(inputDto, TransactionEntity.class);
        verify(transactionDao, times(1)).createTransaction(mappedEntity);
    }
}
