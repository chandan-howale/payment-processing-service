package com.chandan.payments.util;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JsonUtil {
	
	private final ObjectMapper objectMapper;
	
	// want 2 methods : toJson and fromJson
	
	public String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			log.error("Error converting object to JSON string", e);
			throw new RuntimeException("JSON conversion error: " + e.getMessage());
		}
	}
	
	public <T> T fromJson(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (Exception e) {
			log.error("Error converting JSON string to object", e);
			throw new RuntimeException("JSON conversion error: " + e.getMessage());
		}
	}

}
