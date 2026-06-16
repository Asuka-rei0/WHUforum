package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openisle.model.TreeholeRiskLevel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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

  @Test
  void assessTreeholeRetriesTransportFailures() {
    RetryRestTemplate restTemplate = new RetryRestTemplate(2);
    OpenAiCompatibleAiReviewClient client = new OpenAiCompatibleAiReviewClient(
      new ObjectMapper(),
      restTemplate,
      0
    );
    ReflectionTestUtils.setField(client, "apiKey", "test-key");
    ReflectionTestUtils.setField(client, "baseUrl", "https://api.deepseek.com/v1");

    AiReviewResult result = client.assessTreehole(new AiReviewRequest(1L, "title", "content"));

    assertEquals(TreeholeRiskLevel.L0, result.riskLevel());
    assertEquals(3, restTemplate.calls);
  }

  @Test
  void assessTreeholeStopsAfterTransportRetryLimit() {
    RetryRestTemplate restTemplate = new RetryRestTemplate(3);
    OpenAiCompatibleAiReviewClient client = new OpenAiCompatibleAiReviewClient(
      new ObjectMapper(),
      restTemplate,
      0
    );
    ReflectionTestUtils.setField(client, "apiKey", "test-key");
    ReflectionTestUtils.setField(client, "baseUrl", "https://api.deepseek.com/v1");

    assertThrows(
      ResourceAccessException.class,
      () -> client.assessTreehole(new AiReviewRequest(1L, "title", "content"))
    );
    assertEquals(3, restTemplate.calls);
  }

  private static final class RetryRestTemplate extends RestTemplate {

    private final int failuresBeforeSuccess;
    private int calls;

    private RetryRestTemplate(int failuresBeforeSuccess) {
      this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> exchange(
      String url,
      HttpMethod method,
      HttpEntity<?> requestEntity,
      Class<T> responseType,
      Object... uriVariables
    ) {
      calls++;
      if (calls <= failuresBeforeSuccess) {
        throw new ResourceAccessException("temporary handshake failure");
      }
      Map<String, Object> message = Map.of(
        "content",
        "{\"riskLevel\":\"L0\",\"reason\":\"normal\",\"recommendedAction\":\"publish\"}"
      );
      Map<String, Object> choice = Map.of("message", message);
      return (ResponseEntity<T>) ResponseEntity.ok(Map.of("choices", List.of(choice)));
    }
  }
}
