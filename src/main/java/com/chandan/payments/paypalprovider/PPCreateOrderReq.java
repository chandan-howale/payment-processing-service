package com.chandan.payments.paypalprovider;

import lombok.Data;

@Data
public class PPCreateOrderReq {
	private String currencyCode;
	private Double amount;
	private String returnUrl;
	private String cancelUrl;
}
