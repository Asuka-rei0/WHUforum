package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openisle.model.TreeholeRiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpenAiCompatibleAiReviewClientTest {

  @Test
  void chatCompletionsUrlAppendsEndpointForBaseUrl() {
    OpenAiCompatibleAiReviewClient client = new OpenAiCompatibleAiReviewClient(new ObjectMapper());
    ReflectionTestUtils.setField(client, "baseUrl", "https://api.deepseek.com/v1");

    assertEquals("https://api.deepseek.com/v1/chat/completions", client.chatCompletionsUrl());
  }

  @Test
  void chatCompletionsUrlAcceptsFullEndpoint() {
    OpenAiCompatibleAiReviewClient client = new OpenAiCompatibleAiReviewClient(new ObjectMapper());
    ReflectionTestUtils.setField(
      client,
      "baseUrl",
      "https://api.deepseek.com/v1/chat/completions/"
    );

    assertEquals("https://api.deepseek.com/v1/chat/completions", client.chatCompletionsUrl());
  }

  @Test
  void parseReviewResultReadsJsonFromModelContent() {
    OpenAiCompatibleAiReviewClient client = new OpenAiCompatibleAiReviewClient(new ObjectMapper());

    AiReviewResult result = client.parseReviewResult(
      """
      ```json
      {"riskLevel":"L3","reason":"high risk","recommendedAction":"restrict"}
      ```
      """
    );

    assertEquals(TreeholeRiskLevel.L3, result.riskLevel());
    assertEquals("high risk", result.reason());
    assertEquals("restrict", result.recommendedAction());
  }
}
