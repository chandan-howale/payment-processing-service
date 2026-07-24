package com.chandan.payments.service.interfaces;

import com.chandan.payments.pojo.CreatePaymentRequest;
import com.chandan.payments.pojo.InitiatePaymentRequest;
import com.chandan.payments.pojo.PaymentResponse;

public interface PaymentService {
	
	public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);
	
	public PaymentResponse initiatePayment(String txnReference, InitiatePaymentRequest initiatePaymentRequest);
	
	public PaymentResponse capturePayment(String txnReference);



}
