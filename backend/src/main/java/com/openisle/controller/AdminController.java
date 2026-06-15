package com.openisle.controller;

import com.openisle.dto.AnonymousAuditDto;
import com.openisle.dto.EmailTestRequest;
import com.openisle.dto.SensitiveWordDto;
import com.openisle.dto.SensitiveWordRequest;
import com.openisle.exception.EmailSendException;
import com.openisle.service.AnonymousAuditService;
import com.openisle.service.EmailSender;
import com.openisle.service.SensitiveWordService;
import com.openisle.service.TreeholeInterventionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple admin demo endpoint.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminController {

  private static final SecureRandom TEST_CODE_RANDOM = new SecureRandom();

  private final AnonymousAuditService anonymousAuditService;
  private final SensitiveWordService sensitiveWordService;
  private final EmailSender emailSender;
  private final TreeholeInterventionService treeholeInterventionService;

  @GetMapping("/api/admin/hello")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Admin greeting", description = "Returns a greeting for admin users")
  @ApiResponse(
    responseCode = "200",
    description = "Greeting payload",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public Map<String, String> adminHello() {
    return Map.of("message", "Hello, Admin User");
  }

  @GetMapping("/api/admin/anonymous/posts/{postId}")
  @SecurityRequirement(name = "JWT")
  @Operation(
    summary = "List anonymous audit records",
    description = "Resolve anonymous authors for a post"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Anonymous audit records",
    content = @Content(schema = @Schema(implementation = AnonymousAuditDto.class))
  )
  public List<AnonymousAuditDto> listAnonymousAudit(
    @PathVariable Long postId,
    Authentication auth
  ) {
    if (auth != null) {
      treeholeInterventionService.recordLegacyAnonymousReveal(postId, auth.getName());
    }
    return anonymousAuditService.listByPost(postId);
  }

  @GetMapping("/api/admin/sensitive-words")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "List sensitive words", description = "List moderation sensitive words")
  @ApiResponse(
    responseCode = "200",
    description = "Sensitive words",
    content = @Content(schema = @Schema(implementation = SensitiveWordDto.class))
  )
  public List<SensitiveWordDto> listSensitiveWords() {
    return sensitiveWordService.list();
  }

  @PostMapping("/api/admin/email/test")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Send test email", description = "Send an email delivery test code")
  @ApiResponse(
    responseCode = "200",
    description = "Email test accepted by sender",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> sendEmailTest(@RequestBody EmailTestRequest request) {
    String to = normalizeEmail(request == null ? null : request.getTo());
    if (!isWhuEmail(to)) {
      return ResponseEntity.badRequest().body(
        Map.of(
          "field",
          "to",
          "error",
          "Only @whu.edu.cn email delivery tests are supported",
          "reason_code",
          "WHU_EMAIL_REQUIRED"
        )
      );
    }

    String code = String.format("%06d", TEST_CODE_RANDOM.nextInt(1000000));
    try {
      emailSender.sendEmail(
        to,
        "珞珈论坛邮件投递测试",
        "你的珞珈论坛测试验证码是：" +
          code +
          "。如果你收到这封邮件，说明当前发信配置可用于真实注册验证码。",
        Map.of(
          "_purpose",
          "test",
          "username",
          "管理员测试",
          "email",
          to,
          "code",
          code,
          "token",
          code,
          "activateUrl",
          ""
        )
      );
    } catch (EmailSendException e) {
      log.warn("Admin email delivery test failed for {}: {}", to, e.getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
        Map.of(
          "error",
          "测试邮件发送失败，请检查邮件服务配置。",
          "reason_code",
          "EMAIL_SEND_FAILED"
        )
      );
    }

    return ResponseEntity.ok(
      Map.of(
        "sent",
        true,
        "to",
        to,
        "testCode",
        code,
        "message",
        "测试邮件已提交给邮件服务，请到邮箱核对 testCode。"
      )
    );
  }

  @PostMapping("/api/admin/sensitive-words")
  @SecurityRequirement(name = "JWT")
  @Operation(
    summary = "Upsert sensitive word",
    description = "Create or update a moderation sensitive word"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Saved sensitive word",
    content = @Content(schema = @Schema(implementation = SensitiveWordDto.class))
  )
  public ResponseEntity<SensitiveWordDto> upsertSensitiveWord(
    @RequestBody SensitiveWordRequest request
  ) {
    return ResponseEntity.ok(sensitiveWordService.upsert(request));
  }

  @DeleteMapping("/api/admin/sensitive-words/{id}")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Delete sensitive word", description = "Delete a moderation sensitive word")
  @ApiResponse(responseCode = "200", description = "Deleted sensitive word")
  public ResponseEntity<Void> deleteSensitiveWord(@PathVariable Long id) {
    sensitiveWordService.delete(id);
    return ResponseEntity.ok().build();
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private boolean isWhuEmail(String email) {
    return email.endsWith("@whu.edu.cn");
  }
}
