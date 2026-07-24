package com.chandan.payments.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.chandan.payments.dao.interfaces.TransactionDao;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.http.HttpServiceEngine;
import com.chandan.payments.pojo.CreatePaymentRequest;
import com.chandan.payments.pojo.PaymentResponse;
import com.chandan.payments.service.PaymentStatusService;
import com.chandan.payments.service.helper.PPCaptureOrderHelper;
import com.chandan.payments.service.helper.PPCreateOrderHelper;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class PaymentServiceImplTest {
	
	@Mock
	private PPCreateOrderHelper ppCreateOrderHelper;
	
	@Mock
	private PPCaptureOrderHelper ppCaptureOrderHelper;
	
	@Mock
	private HttpServiceEngine httpServiceEngine;
	
	@Mock
	private PaymentStatusService paymentStatusService;
	
	@Mock
	private ModelMapper modelMapper;
	
	@Mock
	private TransactionDao transactionDao;
	
	@InjectMocks
	private PaymentServiceImpl paymentServiceImpl;
	
	@Test
	public void testCreatetxn() {
		
		// "Arrange" Arrange the data to calling functional method
		CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        TransactionDto mappedDto = new TransactionDto();
        TransactionDto processedDto = new TransactionDto();
		
		when(modelMapper.map(createPaymentRequest, TransactionDto.class))
        .thenReturn(mappedDto);
		
		when(paymentStatusService.processPayment(any(TransactionDto.class)))
        .thenReturn(processedDto);
		
        processedDto.setTxnReference("generated-ref-123");
        processedDto.setTxnStatusId(1);
		
		
		// "Act" Call the functional method to be tested
		PaymentResponse response = paymentServiceImpl.createPayment(createPaymentRequest);
		
        // Assert - Basic
        assertNotNull(response);
        assertEquals("generated-ref-123", response.getTxnReference());
        assertEquals(1, response.getTxnStatusId());
        
        // Verify modelMapper called once
        verify(modelMapper, times(1))
                .map(createPaymentRequest, TransactionDto.class);
        
        // Capture argument passed to processPayment
        ArgumentCaptor<TransactionDto> captor =
                ArgumentCaptor.forClass(TransactionDto.class);

        verify(paymentStatusService, times(1))
                .processPayment(captor.capture());

        TransactionDto capturedDto = captor.getValue();
        
        // Strong verification
        assertEquals(1, capturedDto.getTxnStatusId());
        assertNotNull(capturedDto.getTxnReference());
        assertEquals(36, capturedDto.getTxnReference().length());
        
        // Ensure no extra interactions
        verifyNoMoreInteractions(modelMapper, paymentStatusService);
        
		log.info("Test Method executed..!");
	}

}
