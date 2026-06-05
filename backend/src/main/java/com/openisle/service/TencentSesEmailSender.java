package com.openisle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openisle.exception.EmailSendException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ses.v20201002.SesClient;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import com.tencentcloudapi.ses.v20201002.models.SendEmailResponse;
import com.tencentcloudapi.ses.v20201002.models.Template;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "tencent-ses")
public class TencentSesEmailSender extends EmailSender {

  private static final Pattern SIX_DIGIT_CODE = Pattern.compile("\\b(\\d{6})\\b");

  private final String secretId;
  private final String secretKey;
  private final String region;
  private final String endpoint;
  private final String fromEmailAddress;
  private final String fromName;
  private final String templateId;
  private final String registerTemplateId;
  private final String resetTemplateId;
  private final String testTemplateId;
  private final String replyToAddresses;
  private final long triggerType;
  private final ObjectMapper objectMapper;
  private final TencentSesApi api;

  @Autowired
  public TencentSesEmailSender(
    @Value("${tencent.ses.secret-id:}") String secretId,
    @Value("${tencent.ses.secret-key:}") String secretKey,
    @Value("${tencent.ses.region:ap-guangzhou}") String region,
    @Value("${tencent.ses.endpoint:ses.tencentcloudapi.com}") String endpoint,
    @Value("${tencent.ses.from-email:}") String fromEmailAddress,
    @Value("${app.email.from-name:WHUforum}") String fromName,
    @Value("${tencent.ses.template-id:}") String templateId,
    @Value("${tencent.ses.register-template-id:}") String registerTemplateId,
    @Value("${tencent.ses.reset-template-id:}") String resetTemplateId,
    @Value("${tencent.ses.test-template-id:}") String testTemplateId,
    @Value("${tencent.ses.reply-to:}") String replyToAddresses,
    @Value("${tencent.ses.trigger-type:1}") long triggerType,
    ObjectMapper objectMapper
  ) {
    this(
      secretId,
      secretKey,
      region,
      endpoint,
      fromEmailAddress,
      fromName,
      templateId,
      registerTemplateId,
      resetTemplateId,
      testTemplateId,
      replyToAddresses,
      triggerType,
      objectMapper,
      null
    );
  }

  TencentSesEmailSender(
    String secretId,
    String secretKey,
    String region,
    String endpoint,
    String fromEmailAddress,
    String fromName,
    String templateId,
    String registerTemplateId,
    String resetTemplateId,
    String testTemplateId,
    String replyToAddresses,
    long triggerType,
    ObjectMapper objectMapper,
    TencentSesApi api
  ) {
    this.secretId = normalize(secretId);
    this.secretKey = normalize(secretKey);
    this.region = normalize(region).isBlank() ? "ap-guangzhou" : normalize(region);
    this.endpoint = normalize(endpoint).isBlank() ? "ses.tencentcloudapi.com" : normalize(endpoint);
    this.fromEmailAddress = normalize(fromEmailAddress);
    this.fromName = normalize(fromName).isBlank() ? "WHUforum" : normalize(fromName);
    this.templateId = normalize(templateId);
    this.registerTemplateId = normalize(registerTemplateId);
    this.resetTemplateId = normalize(resetTemplateId);
    this.testTemplateId = normalize(testTemplateId);
    this.replyToAddresses = normalize(replyToAddresses);
    this.triggerType = triggerType;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    this.api = api == null ? request -> createClient().SendEmail(request) : api;
  }

  @Override
  public void sendEmail(String to, String subject, String text) {
    sendEmail(to, subject, text, fallbackTemplateData(to, text));
  }

  @Override
  public void sendEmail(String to, String subject, String text, Map<String, String> templateData) {
    validateConfig();
    Map<String, String> data = fallbackTemplateData(to, text);
    if (templateData != null) {
      templateData.forEach((key, value) -> {
        if (key != null && value != null) {
          data.put(key, value);
        }
      });
    }
    String purpose = data.remove("_purpose");
    Long resolvedTemplateId = parseTemplateId(resolveTemplateId(purpose));

    Template template = new Template();
    template.setTemplateID(resolvedTemplateId);
    template.setTemplateData(toJson(data));

    SendEmailRequest request = new SendEmailRequest();
    request.setFromEmailAddress(formatFromEmailAddress());
    request.setDestination(new String[] { to });
    request.setSubject(subject);
    request.setTemplate(template);
    request.setTriggerType(triggerType);
    if (!replyToAddresses.isBlank()) {
      request.setReplyToAddresses(replyToAddresses);
    }

    try {
      SendEmailResponse response = api.send(request);
      if (response == null || normalize(response.getMessageId()).isBlank()) {
        throw new EmailSendException("Tencent SES did not return MessageId");
      }
    } catch (TencentCloudSDKException e) {
      throw new EmailSendException("Tencent SES send failed: " + e.getMessage(), e);
    }
  }

  private SesClient createClient() {
    Credential credential = new Credential(secretId, secretKey);
    HttpProfile httpProfile = new HttpProfile();
    httpProfile.setEndpoint(endpoint);
    ClientProfile clientProfile = new ClientProfile();
    clientProfile.setHttpProfile(httpProfile);
    return new SesClient(credential, region, clientProfile);
  }

  private void validateConfig() {
    if (secretId.isBlank()) {
      throw new EmailSendException("TENCENTCLOUD_SECRET_ID is not configured");
    }
    if (secretKey.isBlank()) {
      throw new EmailSendException("TENCENTCLOUD_SECRET_KEY is not configured");
    }
    if (fromEmailAddress.isBlank()) {
      throw new EmailSendException("TENCENT_SES_FROM_EMAIL is not configured");
    }
    if (templateId.isBlank() && registerTemplateId.isBlank()) {
      throw new EmailSendException("TENCENT_SES_TEMPLATE_ID is not configured");
    }
  }

  private String resolveTemplateId(String purpose) {
    if ("register".equals(purpose) && !registerTemplateId.isBlank()) {
      return registerTemplateId;
    }
    if ("reset_password".equals(purpose)) {
      if (!resetTemplateId.isBlank()) {
        return resetTemplateId;
      }
      throw new EmailSendException("TENCENT_SES_RESET_TEMPLATE_ID is not configured");
    }
    if ("test".equals(purpose) && !testTemplateId.isBlank()) {
      return testTemplateId;
    }
    return templateId.isBlank() ? registerTemplateId : templateId;
  }

  private Long parseTemplateId(String rawTemplateId) {
    try {
      return Long.valueOf(rawTemplateId);
    } catch (NumberFormatException e) {
      throw new EmailSendException("TENCENT_SES_TEMPLATE_ID must be a number", e);
    }
  }

  private String toJson(Map<String, String> data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new EmailSendException("Failed to serialize Tencent SES TemplateData", e);
    }
  }

  private Map<String, String> fallbackTemplateData(String to, String text) {
    String codeOrToken = extractSixDigitCode(text);
    Map<String, String> data = new LinkedHashMap<>();
    data.put("username", usernameFromEmail(to));
    data.put("email", normalize(to));
    data.put("code", codeOrToken);
    data.put("token", codeOrToken);
    data.put("activateUrl", "");
    data.put("resetUrl", "");
    data.put("content", text == null ? "" : text);
    return data;
  }

  private String formatFromEmailAddress() {
    if (fromEmailAddress.contains("<") || fromName.isBlank()) {
      return fromEmailAddress;
    }
    return fromName + " <" + fromEmailAddress + ">";
  }

  private String extractSixDigitCode(String text) {
    if (text == null) {
      return "";
    }
    Matcher matcher = SIX_DIGIT_CODE.matcher(text);
    return matcher.find() ? matcher.group(1) : "";
  }

  private String usernameFromEmail(String email) {
    String normalized = normalize(email);
    int at = normalized.indexOf('@');
    return at > 0 ? normalized.substring(0, at) : normalized;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}

@FunctionalInterface
interface TencentSesApi {
  SendEmailResponse send(SendEmailRequest request) throws TencentCloudSDKException;
}
