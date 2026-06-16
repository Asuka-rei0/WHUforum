package com.openisle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openisle.model.TreeholeRiskLevel;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OpenAiCompatibleAiReviewClient implements AiReviewClient {

  static {
    // DeepSeek's current edge can close Java 17 TLS 1.3 POST handshakes; TLS 1.2 is verified.
    System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
    System.setProperty("https.protocols", "TLSv1.2");
  }

  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate = new RestTemplate(treeholeAiRequestFactory());

  @Value("${app.treehole.ai-review.api-key:}")
  private String apiKey;

  @Value("${app.treehole.ai-review.model:deepseek-v4-flash}")
  private String model;

  @Value("${app.treehole.ai-review.base-url:https://api.deepseek.com/v1}")
  private String baseUrl;

  @Override
  public AiReviewResult assessTreehole(AiReviewRequest request) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Treehole AI review API key is not configured");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("temperature", 0);
    body.put("messages", buildMessages(request));

    ResponseEntity<Map> response = restTemplate.exchange(
      chatCompletionsUrl(),
      HttpMethod.POST,
      new HttpEntity<>(body, headers),
      Map.class
    );
    String content = extractContent(response.getBody());
    return parseReviewResult(content);
  }

  String chatCompletionsUrl() {
    String normalized = baseUrl == null ? "" : baseUrl.trim();
    if (normalized.isBlank()) {
      normalized = "https://api.deepseek.com/v1";
    }
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (normalized.endsWith("/chat/completions")) {
      return normalized;
    }
    return normalized + "/chat/completions";
  }

  private List<Map<String, String>> buildMessages(AiReviewRequest request) {
    List<Map<String, String>> messages = new ArrayList<>();
    messages.add(
      Map.of(
        "role",
        "system",
        "content",
        """
        You are a safety and mental-risk content reviewer for an anonymous campus forum.
        Do not diagnose mental illness. Only assess publication safety and psychological risk.
        Risk levels:
        L0 normal content.
        L1 low risk, mild negative emotion without immediate safety concern.
        L2 medium risk, concerning distress that needs administrator review before public exposure.
        L3 high risk, likely self-harm/violence/crisis content that must be restricted and reported.
        L4 urgent risk, imminent self-harm/violence or emergency signal that must be restricted and reported immediately.
        Return JSON only: {"riskLevel":"L0|L1|L2|L3|L4","reason":"short reason","recommendedAction":"short action"}.
        """
      )
    );
    messages.add(
      Map.of(
        "role",
        "user",
        "content",
        "Title:\n" +
          nullToEmpty(request.title()) +
          "\n\nContent:\n" +
          nullToEmpty(request.content())
      )
    );
    return messages;
  }

  private String extractContent(Map responseBody) {
    if (responseBody == null) {
      throw new IllegalStateException("AI review response body is empty");
    }
    Object choicesObj = responseBody.get("choices");
    if (!(choicesObj instanceof List choices) || choices.isEmpty()) {
      throw new IllegalStateException("AI review response has no choices");
    }
    Object first = choices.get(0);
    if (!(first instanceof Map firstMap)) {
      throw new IllegalStateException("AI review response choice is invalid");
    }
    Object messageObj = firstMap.get("message");
    if (!(messageObj instanceof Map message)) {
      throw new IllegalStateException("AI review response message is invalid");
    }
    Object content = message.get("content");
    if (!(content instanceof String text) || text.isBlank()) {
      throw new IllegalStateException("AI review response content is empty");
    }
    return text.trim();
  }

  AiReviewResult parseReviewResult(String content) {
    try {
      JsonNode node = objectMapper.readTree(extractJsonObject(content));
      TreeholeRiskLevel riskLevel = TreeholeRiskLevel.valueOf(
        node.path("riskLevel").asText("").trim().toUpperCase(Locale.ROOT)
      );
      return new AiReviewResult(
        riskLevel,
        truncate(node.path("reason").asText(""), 1000),
        truncate(node.path("recommendedAction").asText(""), 1000)
      );
    } catch (Exception e) {
      throw new IllegalStateException("AI review response is not valid JSON", e);
    }
  }

  private String extractJsonObject(String content) {
    String trimmed = content == null ? "" : content.trim();
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start < 0 || end <= start) {
      throw new IllegalStateException("AI review response has no JSON object");
    }
    return trimmed.substring(start, end + 1);
  }

  private String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static SimpleClientHttpRequestFactory treeholeAiRequestFactory() {
    Tls12RequestFactory factory = new Tls12RequestFactory();
    factory.setConnectTimeout(10000);
    factory.setReadTimeout(30000);
    return factory;
  }

  private static final class Tls12RequestFactory extends SimpleClientHttpRequestFactory {

    private final SSLSocketFactory sslSocketFactory = createTls12SocketFactory();

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod)
      throws IOException {
      if (connection instanceof HttpsURLConnection httpsConnection) {
        httpsConnection.setSSLSocketFactory(sslSocketFactory);
      }
      super.prepareConnection(connection, httpMethod);
    }
  }

  private static SSLSocketFactory createTls12SocketFactory() {
    try {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
      return new Tls12SocketFactory(context.getSocketFactory());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Unable to initialize TLSv1.2 context", e);
    }
  }

  private static final class Tls12SocketFactory extends SSLSocketFactory {

    private static final String[] TLS12_ONLY = { "TLSv1.2" };

    private final SSLSocketFactory delegate;

    private Tls12SocketFactory(SSLSocketFactory delegate) {
      this.delegate = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
      return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
      return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
      throws IOException {
      return enforceTls12(delegate.createSocket(socket, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
      return enforceTls12(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
      throws IOException {
      return enforceTls12(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      return enforceTls12(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(
      InetAddress address,
      int port,
      InetAddress localAddress,
      int localPort
    ) throws IOException {
      return enforceTls12(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket enforceTls12(Socket socket) {
      if (socket instanceof SSLSocket sslSocket) {
        sslSocket.setEnabledProtocols(TLS12_ONLY);
      }
      return socket;
    }
  }
}
