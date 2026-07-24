package com.chandan.payments.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Unit tests for {@link AdditionController}.
 *
 * Simple GET endpoint that adds two numbers.
 * Uses @WebMvcTest to load only the web layer.
 */
@WebMvcTest(AdditionController.class)
class AdditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void add_returnsSumOfTwoNumbers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/add")
                .param("num1", "10")
                .param("num2", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string("30"));
    }

    @Test
    void add_handlesZeroCorrectly() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/add")
                .param("num1", "0")
                .param("num2", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void add_handlesNegativeNumbers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/add")
                .param("num1", "-5")
                .param("num2", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string("-2"));
    }
}