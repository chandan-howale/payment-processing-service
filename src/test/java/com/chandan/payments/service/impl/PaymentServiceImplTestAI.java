package com.chandan.payments.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.chandan.payments.dao.interfaces.TransactionDao;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.entity.TransactionEntity;
import com.chandan.payments.exception.ProcessingServiceException;
import com.chandan.payments.http.HttpRequest;
import com.chandan.payments.http.HttpServiceEngine;
import com.chandan.payments.paypalprovider.PPOrderResponse;
import com.chandan.payments.pojo.CreatePaymentRequest;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.pojo.PaymentResponse;
import com.chandan.payments.service.PaymentStatusService;
import com.chandan.payments.service.helper.PPCaptureOrderHelper;
import com.chandan.payments.service.helper.PPCreateOrderHelper;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTestAI {

    @Mock private PPCreateOrderHelper ppCreateOrderHelper;
    @Mock private PPCaptureOrderHelper ppCaptureOrderHelper;
    @Mock private HttpServiceEngine httpServiceEngine;
    @Mock private PaymentStatusService paymentStatusService;
    @Mock private ModelMapper modelMapper;
    @Mock private TransactionDao transactionDao;

    @InjectMocks
    private PaymentServiceImpl paymentServiceImpl;

    // =========================================================
    // 1️ createPayment - SUCCESS
    // =========================================================
    @Test
    void testCreatePayment_Success() {

        CreatePaymentRequest request = new CreatePaymentRequest();
        TransactionDto mappedDto = new TransactionDto();
        TransactionDto processedDto = new TransactionDto();

        when(modelMapper.map(request, TransactionDto.class))
                .thenReturn(mappedDto);

        when(paymentStatusService.processPayment(any(TransactionDto.class)))
                .thenReturn(processedDto);

        processedDto.setTxnReference("generated-ref-123");
        processedDto.setTxnStatusId(1);

        PaymentResponse response = paymentServiceImpl.createPayment(request);

        assertNotNull(response);
        assertEquals(1, response.getTxnStatusId());
        assertEquals("generated-ref-123", response.getTxnReference());

        verify(modelMapper, times(1))
                .map(request, TransactionDto.class);

        ArgumentCaptor<TransactionDto> captor =
                ArgumentCaptor.forClass(TransactionDto.class);

        verify(paymentStatusService, times(1))
                .processPayment(captor.capture());

        TransactionDto capturedDto = captor.getValue();

        assertEquals(1, capturedDto.getTxnStatusId());
        assertNotNull(capturedDto.getTxnReference());
        assertEquals(36, capturedDto.getTxnReference().length());

        verifyNoMoreInteractions(modelMapper, paymentStatusService);
    }

    // =========================================================
    // 2️ initiatePayment - SUCCESS FLOW
    // =========================================================
    @Test
    void testInitiatePayment_Success() {

        String txnRef = "txn-123";
        InitiatePaymentRequest initiateRequest = new InitiatePaymentRequest();

        TransactionEntity entity = new TransactionEntity();
        TransactionDto dto = new TransactionDto();
        dto.setTxnReference(txnRef);

        when(transactionDao.getTransactionByTxnReference(txnRef))
                .thenReturn(entity);

        when(modelMapper.map(entity, TransactionDto.class))
                .thenReturn(dto);

        when(paymentStatusService.processPayment(any()))
                .thenReturn(dto);

        HttpRequest httpRequest = new HttpRequest();
        when(ppCreateOrderHelper.prepareHttpRequest(any(), any(), any()))
                .thenReturn(httpRequest);

        ResponseEntity<String> httpResponse = ResponseEntity.ok("success");
        when(httpServiceEngine.makeHttpCall(httpRequest))
                .thenReturn(httpResponse);

        PPOrderResponse ppResponse = new PPOrderResponse();
        ppResponse.setOrderId("order-123");
        ppResponse.setRedirectUrl("http://redirect");
        when(ppCreateOrderHelper.processResponse(httpResponse))
                .thenReturn(ppResponse);

        PaymentResponse response =
                paymentServiceImpl.initiatePayment(txnRef, initiateRequest);

        assertNotNull(response);
        assertEquals(3, response.getTxnStatusId());
        assertEquals("order-123", response.getProviderReference());
        assertEquals("http://redirect", response.getRedirectUrl());

        verify(transactionDao).getTransactionByTxnReference(txnRef);
        verify(paymentStatusService, times(2)).processPayment(any());
        verify(httpServiceEngine).makeHttpCall(httpRequest);
    }

    // =========================================================
    // 3️ initiatePayment - EXCEPTION FLOW
    // =========================================================
    @Test
    void testInitiatePayment_WhenProcessingFails_ShouldUpdateFailedAndThrow() {

        String txnRef = "txn-123";
        InitiatePaymentRequest initiateRequest = new InitiatePaymentRequest();

        TransactionEntity entity = new TransactionEntity();
        TransactionDto dto = new TransactionDto();

        when(transactionDao.getTransactionByTxnReference(txnRef))
                .thenReturn(entity);

        when(modelMapper.map(entity, TransactionDto.class))
                .thenReturn(dto);

        when(paymentStatusService.processPayment(any()))
                .thenReturn(dto);

        HttpRequest httpRequest = new HttpRequest();
        when(ppCreateOrderHelper.prepareHttpRequest(any(), any(), any()))
                .thenReturn(httpRequest);

        ProcessingServiceException ex =
                new ProcessingServiceException("ERR001", "Failure", HttpStatus.INTERNAL_SERVER_ERROR);

        when(httpServiceEngine.makeHttpCall(httpRequest))
                .thenThrow(ex);

        assertThrows(ProcessingServiceException.class, () -> paymentServiceImpl.initiatePayment(txnRef, initiateRequest));

        ArgumentCaptor<TransactionDto> captor =
                ArgumentCaptor.forClass(TransactionDto.class);

        verify(paymentStatusService, atLeastOnce())
                .processPayment(captor.capture());

        TransactionDto failedDto = captor.getValue();
        assertEquals(6, failedDto.getTxnStatusId());
        assertEquals("ERR001", failedDto.getErrorCode());
        assertEquals("Failure", failedDto.getErrorMessage());
    }

    // =========================================================
    // 4️ capturePayment - SUCCESS FLOW
    // =========================================================
    @Test
    void testCapturePayment_Success() {

        String txnRef = "txn-123";

        TransactionEntity entity = new TransactionEntity();
        TransactionDto dto = new TransactionDto();
        dto.setTxnReference(txnRef);

        when(transactionDao.getTransactionByTxnReference(txnRef))
                .thenReturn(entity);

        when(modelMapper.map(entity, TransactionDto.class))
                .thenReturn(dto);

        when(paymentStatusService.processPayment(any()))
                .thenReturn(dto);

        HttpRequest httpRequest = new HttpRequest();
        when(ppCaptureOrderHelper.prepareHttpRequest(any(), any()))
                .thenReturn(httpRequest);

        ResponseEntity<String> httpResponse = ResponseEntity.ok("success");
        when(httpServiceEngine.makeHttpCall(httpRequest))
                .thenReturn(httpResponse);

        when(ppCaptureOrderHelper.processResponse(httpResponse))
                .thenReturn(new PPOrderResponse());

        PaymentResponse response =
                paymentServiceImpl.capturePayment(txnRef);

        assertNotNull(response);
        assertEquals(5, response.getTxnStatusId());

        verify(paymentStatusService, times(2)).processPayment(any());
    }

    // =========================================================
    // 5️ capturePayment - EXCEPTION FLOW
    // =========================================================
    @Test
    void testCapturePayment_WhenHttpFails_ShouldThrow() {

        String txnRef = "txn-123";

        TransactionEntity entity = new TransactionEntity();
        TransactionDto dto = new TransactionDto();

        when(transactionDao.getTransactionByTxnReference(txnRef))
                .thenReturn(entity);

        when(modelMapper.map(entity, TransactionDto.class))
                .thenReturn(dto);

        when(paymentStatusService.processPayment(any()))
                .thenReturn(dto);

        HttpRequest httpRequest = new HttpRequest();
        when(ppCaptureOrderHelper.prepareHttpRequest(any(), any()))
                .thenReturn(httpRequest);

        when(httpServiceEngine.makeHttpCall(httpRequest))
                .thenThrow(new RuntimeException("HTTP Failure"));

        assertThrows(RuntimeException.class, () -> paymentServiceImpl.capturePayment(txnRef));

        // Ensure status NOT updated to 5
        verify(paymentStatusService, times(1)).processPayment(any());
    }
}