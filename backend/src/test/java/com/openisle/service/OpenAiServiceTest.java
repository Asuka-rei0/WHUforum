package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpenAiServiceTest {

  @Test
  void chatCompletionsUrlAppendsEndpointForBaseUrl() {
    OpenAiService service = new OpenAiService();
    ReflectionTestUtils.setField(service, "baseUrl", "https://api.deepseek.com");

    assertEquals("https://api.deepseek.com/chat/completions", service.chatCompletionsUrl());
  }

  @Test
  void chatCompletionsUrlAcceptsFullEndpoint() {
    OpenAiService service = new OpenAiService();
    ReflectionTestUtils.setField(service, "baseUrl", "https://api.openai.com/v1/chat/completions/");

    assertEquals("https://api.openai.com/v1/chat/completions", service.chatCompletionsUrl());
  }
}
