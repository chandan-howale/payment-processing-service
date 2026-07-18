package com.chandan.payments.service.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chandan.payments.paypalprovider.PPOrderResponse;
import com.chandan.payments.paypalprovider.PPErrorResponse;
import com.chandan.payments.http.HttpRequest;
import com.chandan.payments.http.HttpServiceEngine;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.dto.TransactionDto;
import com.chandan.payments.util.JsonUtil;
import com.chandan.payments.exception.ProcessingServiceException;
import com.chandan.payments.constant.ErrorCodeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;

/**
 * Unit tests for {@link PPCreateOrderHelper}.
 *
 * The helper:
 *   1. Builds an {@link HttpRequest} from {@link InitiatePaymentRequest} and {@link TransactionDto}.
 *   2. Calls {@link HttpServiceEngine#makeHttpCall}.
 *   3. Processes the response via {@code processResponse(ResponseEntity)}.
 *
 * We mock {@link HttpServiceEngine} and {@link JsonUtil} to isolate the helper.
 */
@ExtendWith(MockitoExtension.class)
class PPCreateOrderHelperTest {

    @Mock
    private HttpServiceEngine httpServiceEngine;

    @Mock
    private JsonUtil jsonUtil;

    @InjectMocks
    private PPCreateOrderHelper helper;

    // -------------------------------------------------------------------------
    // 1️⃣  Test that the HttpRequest is built correctly
    // -------------------------------------------------------------------------
    @Test
    void prepareHttpRequest_buildsCorrectRequest() throws Exception {
        // Arrange
        String txnReference = "ref-123";
        InitiatePaymentRequest initReq = new InitiatePaymentRequest();
        initReq.setSuccessUrl("https://example.com/success");
        initReq.setCancelUrl("https://example.com/cancel");

        TransactionDto txnDto = new TransactionDto();
        txnDto.setCurrency("USD");
        txnDto.setAmount(java.math.BigDecimal.valueOf(100.50));

        // Set the URL template via reflection
        java.lang.reflect.Field urlField = PPCreateOrderHelper.class
                .getDeclaredField("paypalProviderCreateOrderUrl");
        urlField.setAccessible(true);
        urlField.set(helper, "https://api.paypal.com/v2/orders");

        // Act
        HttpRequest request = helper.prepareHttpRequest(txnReference, initReq, txnDto);

        // Assert
        assertEquals(HttpMethod.POST, request.getHttpMethod());
        assertEquals("https://api.paypal.com/v2/orders", request.getUrl());
        assertEquals("application/json", request.getHttpHeaders().getContentType().toString());
        assertNotNull(request.getBody());
        String body = (String) request.getBody();
        assertTrue(body.contains("\"currency_code\":\"USD\""));
        assertTrue(body.contains("\"amount\":\"100.5\""));
        assertTrue(body.contains("\"return_url\":\"https://example.com/success\""));
        assertTrue(body.contains("\"cancel_url\":\"https://example.com/cancel\""));
    }

    // -------------------------------------------------------------------------
    // 2️⃣  Successful 200 OK with COMPLETED status
    // -------------------------------------------------------------------------
    @Test
    void processResponse_successfulCompletedOrder() {
        ResponseEntity<String> okResponse = new ResponseEntity<>(
                "{\"order_id\":\"order-123\",\"paypal_status\":\"COMPLETED\",\"redirect_url\":\"https://paypal.com/redirect\"}",
                HttpStatus.OK);

        PPOrderResponse parsed = new PPOrderResponse();
        parsed.setOrderId("order-123");
        parsed.setPaypalStatus("COMPLETED");
        parsed.setRedirectUrl("https://paypal.com/redirect");

        when(jsonUtil.fromJson(okResponse.getBody(), PPOrderResponse.class))
                .thenReturn(parsed);

        PPOrderResponse result = helper.processResponse(okResponse);

        assertNotNull(result);
        assertEquals("order-123", result.getOrderId());
        assertEquals("COMPLETED", result.getPaypalStatus());
        assertEquals("https://paypal.com/redirect", result.getRedirectUrl());
        verify(jsonUtil).fromJson(okResponse.getBody(), PPOrderResponse.class);
    }

    // -------------------------------------------------------------------------
    // 3️⃣  200 OK but missing required fields → throws PAYPAL_PROVIDER_UNKNOWN_ERROR
    // -------------------------------------------------------------------------
    @Test
    void processResponse_okButMissingFields_throwsException() {
        ResponseEntity<String> okResponse = new ResponseEntity<>(
                "{\"order_id\":\"order-456\",\"paypal_status\":\"PENDING\"}",  // missing redirect_url
                HttpStatus.OK);

        PPOrderResponse parsed = new PPOrderResponse();
        parsed.setOrderId("order-456");
        parsed.setPaypalStatus("PENDING");
        // redirect_url is null

        when(jsonUtil.fromJson(okResponse.getBody(), PPOrderResponse.class))
                .thenReturn(parsed);

        ProcessingServiceException ex = assertThrows(
                ProcessingServiceException.class,
                () -> helper.processResponse(okResponse));

        assertEquals(ErrorCodeEnum.PAYPAL_PROVIDER_UNKNOWN_ERROR.getErrorCode(), ex.getErrorCode());
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
    }

    // -------------------------------------------------------------------------
    // 4️⃣  4xx / 5xx error → parses PPErrorResponse and throws
    // -------------------------------------------------------------------------
    @Test
    void processResponse_errorResponse_throwsProcessingServiceException() {
        ResponseEntity<String> errorResponse = new ResponseEntity<>(
                "{\"error_code\":\"CREATE_ERROR\",\"error_message\":\"Create failed\"}",
                HttpStatus.BAD_REQUEST);

        PPErrorResponse errObj = new PPErrorResponse();
        errObj.setErrorCode("CREATE_ERROR");
        errObj.setErrorMessage("Create failed");

        when(jsonUtil.fromJson(errorResponse.getBody(), PPErrorResponse.class))
                .thenReturn(errObj);

        ProcessingServiceException ex = assertThrows(
                ProcessingServiceException.class,
                () -> helper.processResponse(errorResponse));

        assertEquals("CREATE_ERROR", ex.getErrorCode());
        assertEquals("Create failed", ex.getErrorMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(jsonUtil).fromJson(errorResponse.getBody(), PPErrorResponse.class);
    }
}