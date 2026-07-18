package com.chandan.payments.service.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.chandan.payments.paypalprovider.PPOrderResponse;
import com.chandan.payments.paypalprovider.PPErrorResponse;
import com.chandan.payments.http.HttpRequest;
import com.chandan.payments.http.HttpServiceEngine;
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
 * Unit tests for {@link PPCaptureOrderHelper}.
 *
 * The helper:
 *   1. Builds an {@link HttpRequest} for the capture endpoint.
 *   2. Calls {@link HttpServiceEngine#makeHttpCall}.
 *   3. Processes the response via {@code processResponse(ResponseEntity)}.
 *
 * All external calls are mocked.
 */
@ExtendWith(MockitoExtension.class)
class PPCaptureOrderHelperTest {

    @Mock
    private HttpServiceEngine httpServiceEngine;

    @Mock
    private JsonUtil jsonUtil;

    @InjectMocks
    private PPCaptureOrderHelper helper;

    // -------------------------------------------------------------------------
    // 1️⃣  Test that the HttpRequest is built correctly
    // -------------------------------------------------------------------------
    @Test
    void prepareHttpRequest_buildsCorrectRequest() throws Exception {
        String txnReference = "ref-123";
        TransactionDto txnDto = new TransactionDto();
        txnDto.setProviderReference("provider-abc");

        // Set the URL template via reflection
        java.lang.reflect.Field urlField = PPCaptureOrderHelper.class
                .getDeclaredField("paypalProviderCaptureOrderUrlTemplate");
        urlField.setAccessible(true);
        urlField.set(helper, "https://api.paypal.com/v2/orders/{provider-reference}/capture");

        // Act
        HttpRequest request = helper.prepareHttpRequest(txnReference, txnDto);

        // Assert
        assertEquals(HttpMethod.POST, request.getHttpMethod());
        assertEquals("https://api.paypal.com/v2/orders/provider-abc/capture", request.getUrl());
        assertEquals("application/json", request.getHttpHeaders().getContentType().toString());
        assertEquals("", request.getBody());
    }

    // -------------------------------------------------------------------------
    // 2️⃣  Successful 200 OK with COMPLETED status
    // -------------------------------------------------------------------------
    @Test
    void processResponse_successfulCompletedOrder() {
        ResponseEntity<String> okResponse = new ResponseEntity<>(
                "{\"order_id\":\"order-999\",\"paypal_status\":\"COMPLETED\",\"redirect_url\":\"https://paypal.com/redirect\"}",
                HttpStatus.OK);

        PPOrderResponse parsed = new PPOrderResponse();
        parsed.setOrderId("order-999");
        parsed.setPaypalStatus("COMPLETED");
        parsed.setRedirectUrl("https://paypal.com/redirect");

        when(jsonUtil.fromJson(okResponse.getBody(), PPOrderResponse.class))
                .thenReturn(parsed);

        PPOrderResponse result = helper.processResponse(okResponse);

        assertNotNull(result);
        assertEquals("order-999", result.getOrderId());
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
                "{\"order_id\":\"order-888\",\"paypal_status\":\"PENDING\"}",  // missing redirect_url
                HttpStatus.OK);

        PPOrderResponse parsed = new PPOrderResponse();
        parsed.setOrderId("order-888");
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
                "{\"error_code\":\"CAPTURE_ERROR\",\"error_message\":\"Capture failed\"}",
                HttpStatus.BAD_REQUEST);

        PPErrorResponse err = new PPErrorResponse();
        err.setErrorCode("CAPTURE_ERROR");
        err.setErrorMessage("Capture failed");

        when(jsonUtil.fromJson(errorResponse.getBody(), PPErrorResponse.class))
                .thenReturn(err);

        ProcessingServiceException ex = assertThrows(
                ProcessingServiceException.class,
                () -> helper.processResponse(errorResponse));

        assertEquals("CAPTURE_ERROR", ex.getErrorCode());
        assertEquals("Capture failed", ex.getErrorMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(jsonUtil).fromJson(errorResponse.getBody(), PPErrorResponse.class);
    }
}