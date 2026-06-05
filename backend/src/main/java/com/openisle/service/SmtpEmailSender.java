package com.openisle.service;

import com.openisle.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailSender extends EmailSender {

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String fromEmail;
  private final String fromName;
  private final boolean auth;
  private final boolean startTlsEnabled;
  private final boolean sslEnabled;
  private final int connectionTimeout;
  private final int timeout;
  private final int writeTimeout;
  private final JavaMailSender mailSender;

  @Autowired
  public SmtpEmailSender(
    @Value("${smtp.host:}") String host,
    @Value("${smtp.port:587}") int port,
    @Value("${smtp.username:}") String username,
    @Value("${smtp.password:}") String password,
    @Value("${smtp.from.email:}") String fromEmail,
    @Value("${app.email.from-name:WHUforum}") String fromName,
    @Value("${smtp.auth:true}") boolean auth,
    @Value("${smtp.starttls.enable:true}") boolean startTlsEnabled,
    @Value("${smtp.ssl.enable:false}") boolean sslEnabled,
    @Value("${smtp.connection-timeout:10000}") int connectionTimeout,
    @Value("${smtp.timeout:10000}") int timeout,
    @Value("${smtp.write-timeout:10000}") int writeTimeout
  ) {
    this(
      host,
      port,
      username,
      password,
      fromEmail,
      fromName,
      auth,
      startTlsEnabled,
      sslEnabled,
      connectionTimeout,
      timeout,
      writeTimeout,
      null
    );
  }

  SmtpEmailSender(
    String host,
    int port,
    String username,
    String password,
    String fromEmail,
    String fromName,
    boolean auth,
    boolean startTlsEnabled,
    boolean sslEnabled,
    int connectionTimeout,
    int timeout,
    int writeTimeout,
    JavaMailSender mailSender
  ) {
    this.host = normalize(host);
    this.port = port;
    this.username = normalize(username);
    this.password = password == null ? "" : password;
    this.fromEmail = normalize(fromEmail).isBlank() ? this.username : normalize(fromEmail);
    this.fromName = normalize(fromName).isBlank() ? "WHUforum" : normalize(fromName);
    this.auth = auth;
    this.startTlsEnabled = startTlsEnabled;
    this.sslEnabled = sslEnabled;
    this.connectionTimeout = connectionTimeout;
    this.timeout = timeout;
    this.writeTimeout = writeTimeout;
    this.mailSender = mailSender == null ? createMailSender() : mailSender;
  }

  @Override
  public void sendEmail(String to, String subject, String text) {
    validateConfig();
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(
        message,
        false,
        StandardCharsets.UTF_8.name()
      );
      helper.setFrom(fromEmail, fromName);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, false);
      mailSender.send(message);
    } catch (MailException | MessagingException | UnsupportedEncodingException e) {
      throw new EmailSendException("Failed to send email through SMTP: " + e.getMessage(), e);
    }
  }

  private JavaMailSender createMailSender() {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(username);
    sender.setPassword(password);
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

    Properties properties = sender.getJavaMailProperties();
    properties.put("mail.smtp.auth", Boolean.toString(auth));
    properties.put("mail.smtp.starttls.enable", Boolean.toString(startTlsEnabled));
    properties.put("mail.smtp.ssl.enable", Boolean.toString(sslEnabled));
    properties.put("mail.smtp.connectiontimeout", Integer.toString(connectionTimeout));
    properties.put("mail.smtp.timeout", Integer.toString(timeout));
    properties.put("mail.smtp.writetimeout", Integer.toString(writeTimeout));
    return sender;
  }

  private void validateConfig() {
    if (host.isBlank()) {
      throw new EmailSendException("SMTP_HOST is not configured");
    }
    if (fromEmail.isBlank()) {
      throw new EmailSendException("SMTP_FROM_EMAIL or SMTP_USERNAME is not configured");
    }
    if (auth && username.isBlank()) {
      throw new EmailSendException("SMTP_USERNAME is not configured");
    }
    if (auth && password.isBlank()) {
      throw new EmailSendException("SMTP_PASSWORD is not configured");
    }
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
