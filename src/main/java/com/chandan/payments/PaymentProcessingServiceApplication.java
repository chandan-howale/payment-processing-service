package com.chandan.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PaymentProcessingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentProcessingServiceApplication.class, args);
	}

}
