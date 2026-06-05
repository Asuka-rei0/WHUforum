package com.openisle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openisle.exception.EmailSendException;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import com.tencentcloudapi.ses.v20201002.models.SendEmailResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TencentSesEmailSenderTest {

  @Test
  void rejectsMissingSecretIdBeforeCallingTencent() {
    TencentSesEmailSender sender = new TencentSesEmailSender(
      "",
      "secret-key",
      "ap-guangzhou",
      "ses.tencentcloudapi.com",
      "noreply@mail.example.com",
      "WHUforum",
      "100091",
      "",
      "",
      "",
      "",
      1L,
      new ObjectMapper(),
      request -> {
        throw new AssertionError("Tencent API should not be called");
      }
    );

    assertThatThrownBy(() -> sender.sendEmail("student@whu.edu.cn", "subject", "code 123456"))
      .isInstanceOf(EmailSendException.class)
      .hasMessageContaining("TENCENTCLOUD_SECRET_ID");
  }

  @Test
  void sendsTemplateEmailWithActivationVariables() {
    AtomicReference<SendEmailRequest> captured = new AtomicReference<>();
    TencentSesEmailSender sender = new TencentSesEmailSender(
      "secret-id",
      "secret-key",
      "ap-guangzhou",
      "ses.tencentcloudapi.com",
      "noreply@mail.example.com",
      "WHUforum",
      "100091",
      "",
      "",
      "",
      "reply@mail.example.com",
      1L,
      new ObjectMapper(),
      request -> {
        captured.set(request);
        SendEmailResponse response = new SendEmailResponse();
        response.setMessageId("qcloud-message-id");
        return response;
      }
    );

    sender.sendEmail(
      "student@whu.edu.cn",
      "珞珈论坛邮箱验证",
      "fallback",
      Map.of(
        "_purpose",
        "register",
        "username",
        "Katrina",
        "token",
        "activation-token",
        "activateUrl",
        "https://forum.whu.edu.cn/activate?token=activation-token"
      )
    );

    SendEmailRequest request = captured.get();
    assertThat(request.getFromEmailAddress()).isEqualTo("WHUforum <noreply@mail.example.com>");
    assertThat(request.getDestination()).containsExactly("student@whu.edu.cn");
    assertThat(request.getSubject()).isEqualTo("珞珈论坛邮箱验证");
    assertThat(request.getReplyToAddresses()).isEqualTo("reply@mail.example.com");
    assertThat(request.getTriggerType()).isEqualTo(1L);
    assertThat(request.getTemplate().getTemplateID()).isEqualTo(100091L);
    assertThat(request.getTemplate().getTemplateData()).contains(
      "\"username\":\"Katrina\"",
      "\"token\":\"activation-token\"",
      "\"activateUrl\":\"https://forum.whu.edu.cn/activate?token=activation-token\""
    );
    assertThat(request.getTemplate().getTemplateData()).doesNotContain("_purpose");
  }

  @Test
  void sendsResetEmailWithResetTemplateVariables() {
    AtomicReference<SendEmailRequest> captured = new AtomicReference<>();
    TencentSesEmailSender sender = new TencentSesEmailSender(
      "secret-id",
      "secret-key",
      "ap-guangzhou",
      "ses.tencentcloudapi.com",
      "noreply@mail.example.com",
      "WHUforum",
      "100091",
      "",
      "200092",
      "",
      "",
      1L,
      new ObjectMapper(),
      request -> {
        captured.set(request);
        SendEmailResponse response = new SendEmailResponse();
        response.setMessageId("qcloud-message-id");
        return response;
      }
    );

    sender.sendEmail(
      "student@whu.edu.cn",
      "珞珈论坛密码重置",
      "fallback",
      Map.of(
        "_purpose",
        "reset_password",
        "username",
        "Katrina",
        "token",
        "reset-token",
        "resetUrl",
        "https://forum.whu.edu.cn/reset-password?token=reset-token"
      )
    );

    SendEmailRequest request = captured.get();
    assertThat(request.getSubject()).isEqualTo("珞珈论坛密码重置");
    assertThat(request.getTemplate().getTemplateID()).isEqualTo(200092L);
    assertThat(request.getTemplate().getTemplateData()).contains(
      "\"username\":\"Katrina\"",
      "\"token\":\"reset-token\"",
      "\"resetUrl\":\"https://forum.whu.edu.cn/reset-password?token=reset-token\""
    );
    assertThat(request.getTemplate().getTemplateData()).doesNotContain("_purpose");
  }

  @Test
  void resetEmailRequiresResetTemplateId() {
    TencentSesEmailSender sender = new TencentSesEmailSender(
      "secret-id",
      "secret-key",
      "ap-guangzhou",
      "ses.tencentcloudapi.com",
      "noreply@mail.example.com",
      "WHUforum",
      "100091",
      "",
      "",
      "",
      "",
      1L,
      new ObjectMapper(),
      request -> {
        throw new AssertionError("Tencent API should not be called");
      }
    );

    assertThatThrownBy(() ->
      sender.sendEmail(
        "student@whu.edu.cn",
        "珞珈论坛密码重置",
        "fallback",
        Map.of("_purpose", "reset_password", "token", "reset-token")
      )
    )
      .isInstanceOf(EmailSendException.class)
      .hasMessageContaining("TENCENT_SES_RESET_TEMPLATE_ID");
  }
}
