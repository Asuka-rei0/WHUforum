package com.openisle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openisle.exception.EmailSendException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailSenderTest {

  @Test
  void rejectsMissingHostBeforeSending() {
    JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    SmtpEmailSender sender = new SmtpEmailSender(
      "",
      587,
      "noreply@forum.example",
      "secret",
      "",
      "WHUforum",
      true,
      true,
      false,
      10000,
      10000,
      10000,
      mailSender
    );

    assertThatThrownBy(() -> sender.sendEmail("a@whu.edu.cn", "subject", "text"))
      .isInstanceOf(EmailSendException.class)
      .hasMessageContaining("SMTP_HOST");
  }

  @Test
  void sendsMimeMessageThroughConfiguredSmtpSender() throws Exception {
    JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    MimeMessage message = new MimeMessage((Session) null);
    when(mailSender.createMimeMessage()).thenReturn(message);
    SmtpEmailSender sender = new SmtpEmailSender(
      "smtp.example.com",
      587,
      "noreply@forum.example",
      "secret",
      "",
      "WHUforum",
      true,
      true,
      false,
      10000,
      10000,
      10000,
      mailSender
    );

    sender.sendEmail("a@whu.edu.cn", "珞珈论坛邮箱验证", "你的验证码是：123456");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());
    MimeMessage sent = captor.getValue();
    assertThat(sent.getSubject()).isEqualTo("珞珈论坛邮箱验证");
    assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("a@whu.edu.cn");
    InternetAddress from = (InternetAddress) sent.getFrom()[0];
    assertThat(from.getAddress()).isEqualTo("noreply@forum.example");
    assertThat(from.getPersonal()).isEqualTo("WHUforum");
  }
}
