package com.chandan.payments.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chandan.payments.constant.ErrorCodeEnum;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.exception.ProcessingServiceException;
import com.chandan.payments.service.interfaces.TransactionStatusProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link PaymentStatusService}.
 *
 * The service delegates to {@link PaymentStatusFactory} which returns a
 * {@link TransactionStatusProcessor} based on the statusId. The tests verify the
 * happy‑path delegation and the error path when no processor is found.
 */
@ExtendWith(MockitoExtension.class)
class PaymentStatusServiceTest {

    @Mock
    private PaymentStatusFactory paymentStatusFactory;

    private PaymentStatusService service;

    @BeforeEach
    void setUp() {
        service = new PaymentStatusService(paymentStatusFactory);
    }

    @Test
    void processPayment_happyPath_returnsProcessedDto() {
        // Arrange
        TransactionDto input = new TransactionDto();
        input.setTxnStatusId(1);
        input.setTxnReference("ref-123");

        TransactionStatusProcessor processorMock = mock(TransactionStatusProcessor.class);
        TransactionDto processed = new TransactionDto();
        processed.setTxnReference("processed-123");

        when(paymentStatusFactory.getStatusProcessor(1)).thenReturn(processorMock);
        when(processorMock.processStatus(input)).thenReturn(processed);

        // Act
        TransactionDto result = service.processPayment(input);

        // Assert
        assertNotNull(result);
        assertEquals("processed-123", result.getTxnReference());
        verify(paymentStatusFactory, times(1)).getStatusProcessor(1);
        verify(processorMock, times(1)).processStatus(input);
    }

    @Test
    void processPayment_whenProcessorNotFound_throwsException() {
        // Arrange
        TransactionDto input = new TransactionDto();
        input.setTxnStatusId(99);
        when(paymentStatusFactory.getStatusProcessor(99)).thenReturn(null);

        // Act & Assert
        ProcessingServiceException ex = assertThrows(ProcessingServiceException.class,
                () -> service.processPayment(input));
        assertEquals(ErrorCodeEnum.STATUS_NOT_FOUND.getErrorCode(), ex.getErrorCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        verify(paymentStatusFactory, times(1)).getStatusProcessor(99);
    }
}
