package com.chandan.payments.service;


import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.chandan.payments.service.impl.statusprocessors.ApprovedStatusProcessor;
import com.chandan.payments.service.impl.statusprocessors.CreatedStatusProcessor;
import com.chandan.payments.service.impl.statusprocessors.FailedStatusProcessor;
import com.chandan.payments.service.impl.statusprocessors.InitiatedStatusProcessor;
import com.chandan.payments.service.impl.statusprocessors.PendingStatusProcessor;
import com.chandan.payments.service.impl.statusprocessors.SuccessStatusProcessor;
import com.chandan.payments.service.interfaces.TransactionStatusProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusFactory { 
	
	private final ApplicationContext applicationContext;
	
	public TransactionStatusProcessor getStatusProcessor(int statusId) {
		switch (statusId) {
		case 1:
			log.info("Returning CreatedStatusProcessor for statusId: {}", statusId);
			return applicationContext.getBean(CreatedStatusProcessor.class);
		case 2:
			log.info("Returning InitiatedStatusProcessor for statusId: {}", statusId);
			return applicationContext.getBean(InitiatedStatusProcessor.class);
		case 3:
			log.info("Returning PendingStatusProcessor for statusId: {}", statusId);
			return applicationContext.getBean(PendingStatusProcessor.class);
		case 4:
			log.info("Returning ApprovedStatusProcessor for statusId: {}", statusId);
			return applicationContext.getBean(ApprovedStatusProcessor.class);
		case 5:
			log.info("Returning SuccessStatusProcessor for statusId: {}", statusId);
			return applicationContext.getBean(SuccessStatusProcessor.class);
		case 6:
			log.info("Returning FailedStatusProcessor for statusId: {}", statusId);
			return applicationContext.getBean(FailedStatusProcessor.class);
		default:
			log.warn("No processor found for statusId: {}", statusId);
			return null;
		}
	}

}
