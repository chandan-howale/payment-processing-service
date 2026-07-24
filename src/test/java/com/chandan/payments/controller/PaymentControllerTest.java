package com.chandan.payments.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chandan.payments.pojo.CreatePaymentRequest;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.pojo.PaymentResponse;
import com.chandan.payments.service.interfaces.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Unit tests for {@link PaymentController}.
 *
 * Uses @WebMvcTest to load only the web layer and mocks the PaymentService.
 * Tests all three endpoints:
 *   - POST /v1/payments – createPayment
 *   - POST /v1/payments/{txnReference}/initiate – initiatePayment
 *   - POST /v1/payments/{txnReference}/capture – capturePayment
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    // -------------------------------------------------------------------------
    // 1️⃣  POST /v1/payments – createPayment
    // -------------------------------------------------------------------------
    @Test
    void createPayment_returnsPaymentResponse() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(100.0);  // double, not BigDecimal
        request.setCurrency("USD");

        PaymentResponse response = new PaymentResponse();
        response.setTxnReference("txn-123");
        response.setTxnStatusId(1);

        when(paymentService.createPayment(request)).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amount":100.0,"currency":"USD"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnReference").value("txn-123"))
                .andExpect(jsonPath("$.txnStatusId").value(1));

        verify(paymentService).createPayment(request);
    }

    // -------------------------------------------------------------------------
    // 2️⃣  POST /v1/payments/{txnReference}/initiate – initiatePayment
    // -------------------------------------------------------------------------
    @Test
    void initiatePayment_returnsPaymentResponse() throws Exception {
        String txnReference = "txn-456";
        InitiatePaymentRequest initiateRequest = new InitiatePaymentRequest();
        initiateRequest.setSuccessUrl("https://example.com/success");
        initiateRequest.setCancelUrl("https://example.com/cancel");

        PaymentResponse response = new PaymentResponse();
        response.setTxnReference(txnReference);
        response.setRedirectUrl("https://paypal.com/redirect");

        when(paymentService.initiatePayment(txnReference, initiateRequest)).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments/{txnReference}/initiate", txnReference)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"successUrl":"https://example.com/success","cancelUrl":"https://example.com/cancel"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnReference").value(txnReference))
                .andExpect(jsonPath("$.redirectUrl").value("https://paypal.com/redirect"));

        verify(paymentService).initiatePayment(txnReference, initiateRequest);
    }

    // -------------------------------------------------------------------------
    // 3️⃣  POST /v1/payments/{txnReference}/capture – capturePayment
    // -------------------------------------------------------------------------
    @Test
    void capturePayment_returnsPaymentResponse() throws Exception {
        String txnReference = "txn-789";

        PaymentResponse response = new PaymentResponse();
        response.setTxnReference(txnReference);
        response.setTxnStatusId(5);

        when(paymentService.capturePayment(txnReference)).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments/{txnReference}/capture", txnReference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnReference").value(txnReference))
                .andExpect(jsonPath("$.txnStatusId").value(5));

        verify(paymentService).capturePayment(txnReference);
    }
}