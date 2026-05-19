package com.chandan.payments.service.interfaces;

import com.chandan.payments.pojo.CreatePaymentRequest;
import com.chandan.payments.pojo.InitiatePaymentRequest;

public interface PaymentService {
	
	public String createPayment(CreatePaymentRequest createPaymentRequest);
	
	public String initiatePayment(String txnReference, InitiatePaymentRequest initiatePaymentRequest);
	
	public String capturePayment(String txnReference);



}
