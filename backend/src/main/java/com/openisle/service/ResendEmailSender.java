package com.openisle.service;

import com.openisle.exception.EmailSendException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend", matchIfMissing = true)
public class ResendEmailSender extends EmailSender {

  private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";
  private static final String USER_AGENT = "WHUforum/0.0.1";

  private final String apiKey;
  private final String fromEmail;
  private final String fromName;
  private final RestTemplate restTemplate;

  @Autowired
  public ResendEmailSender(
    @Value("${resend.api.key:}") String apiKey,
    @Value("${resend.from.email:}") String fromEmail,
    @Value("${app.email.from-name:WHUforum}") String fromName
  ) {
    this(apiKey, fromEmail, fromName, new RestTemplate());
  }

  ResendEmailSender(String apiKey, String fromEmail, String fromName, RestTemplate restTemplate) {
    this.apiKey = normalize(apiKey);
    this.fromEmail = normalize(fromEmail);
    this.fromName = normalize(fromName).isBlank() ? "WHUforum" : normalize(fromName);
    this.restTemplate = restTemplate;
  }

  @Override
  public void sendEmail(String to, String subject, String text) {
    validateConfig();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);
    headers.set(HttpHeaders.USER_AGENT, USER_AGENT);

    Map<String, String> body = new HashMap<>();
    body.put("to", to);
    body.put("subject", subject);
    body.put("text", text);
    body.put("html", toHtml(text));
    body.put("from", fromName + " <" + fromEmail + ">");

    HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
    try {
      ResponseEntity<String> response = restTemplate.exchange(
        RESEND_EMAILS_URL,
        HttpMethod.POST,
        entity,
        String.class
      );
      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new EmailSendException(messageForStatus(response.getStatusCode().value()));
      }
    } catch (HttpStatusCodeException e) {
      throw new EmailSendException(messageForStatus(e.getStatusCode().value()), e);
    } catch (RestClientException e) {
      throw new EmailSendException("Failed to send email: " + e.getMessage(), e);
    }
  }

  private void validateConfig() {
    if (apiKey.isBlank()) {
      throw new EmailSendException("RESEND_API_KEY is not configured");
    }
    if (fromEmail.isBlank()) {
      throw new EmailSendException("RESEND_FROM_EMAIL is not configured");
    }
  }

  private String messageForStatus(int statusCode) {
    return switch (statusCode) {
      case 401 -> "Resend API key is missing. Check RESEND_API_KEY.";
      case 403 -> "Resend rejected the email request. Check RESEND_API_KEY, RESEND_FROM_EMAIL, and sender domain verification. resend.dev can only send to the Resend account email.";
      case 429 -> "Resend rate limit exceeded. Please retry later.";
      default -> "Email service returned status " + statusCode;
    };
  }

  private String toHtml(String text) {
    String escaped = text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\n", "<br>");
    return "<p>" + escaped + "</p>";
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
