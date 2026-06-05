package com.openisle.service;

import java.util.Map;

/**
 * Abstract email sender used to deliver emails.
 */
public abstract class EmailSender {

  /**
   * Send an email to a recipient.
   * @param to recipient email address
   * @param subject email subject
   * @param text email body
   */
  public abstract void sendEmail(String to, String subject, String text);

  /**
   * Send an email with provider-specific template variables.
   * @param to recipient email address
   * @param subject email subject
   * @param text fallback email body
   * @param templateData variables consumed by template based providers
   */
  public void sendEmail(String to, String subject, String text, Map<String, String> templateData) {
    sendEmail(to, subject, text);
  }
}
