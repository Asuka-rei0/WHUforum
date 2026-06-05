package com.openisle.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.openisle.exception.EmailSendException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class ResendEmailSenderTest {

  @Test
  void rejectsMissingApiKeyBeforeCallingResend() {
    RestTemplate restTemplate = new RestTemplate();
    ResendEmailSender sender = new ResendEmailSender(
      "",
      "noreply@forum.example",
      "WHUforum",
      restTemplate
    );

    assertThatThrownBy(() -> sender.sendEmail("a@whu.edu.cn", "subject", "text"))
      .isInstanceOf(EmailSendException.class)
      .hasMessageContaining("RESEND_API_KEY");
  }

  @Test
  void sendsEmailWithBearerAuthAndUserAgent() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
    ResendEmailSender sender = new ResendEmailSender(
      "re_test",
      "noreply@forum.example",
      "WHUforum",
      restTemplate
    );
    server
      .expect(requestTo("https://api.resend.com/emails"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer re_test"))
      .andExpect(header(HttpHeaders.USER_AGENT, "WHUforum/0.0.1"))
      .andExpect(jsonPath("$.from").value("WHUforum <noreply@forum.example>"))
      .andExpect(jsonPath("$.to").value("a@whu.edu.cn"))
      .andRespond(withSuccess("{\"id\":\"email-id\"}", MediaType.APPLICATION_JSON));

    sender.sendEmail("a@whu.edu.cn", "subject", "text");

    server.verify();
  }

  @Test
  void mapsUnauthorizedResponseToConfigError() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
    ResendEmailSender sender = new ResendEmailSender(
      "re_test",
      "noreply@forum.example",
      "WHUforum",
      restTemplate
    );
    server
      .expect(requestTo("https://api.resend.com/emails"))
      .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThatThrownBy(() -> sender.sendEmail("a@whu.edu.cn", "subject", "text"))
      .isInstanceOf(EmailSendException.class)
      .hasMessageContaining("RESEND_API_KEY");
    server.verify();
  }
}
