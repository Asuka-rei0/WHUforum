package com.openisle.controller;

import com.openisle.config.CachingConfig;
import com.openisle.dto.*;
import com.openisle.exception.EmailSendException;
import com.openisle.exception.FieldException;
import com.openisle.model.RegisterMode;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.*;
import com.openisle.util.VerifyType;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final UserService userService;
  private final JwtService jwtService;
  private final EmailSender emailService;
  private final CaptchaService captchaService;
  private final GithubAuthService githubAuthService;
  private final DiscordAuthService discordAuthService;
  private final TwitterAuthService twitterAuthService;
  private final TelegramAuthService telegramAuthService;
  private final RegisterModeService registerModeService;
  private final NotificationService notificationService;
  private final UserRepository userRepository;
  private final InviteService inviteService;

  @Value("${app.captcha.enabled:false}")
  private boolean captchaEnabled;

  @Value("${app.captcha.register-enabled:false}")
  private boolean registerCaptchaEnabled;

  @Value("${app.captcha.login-enabled:false}")
  private boolean loginCaptchaEnabled;

  @Value("${app.whu.mode:true}")
  private boolean whuMode;

  private boolean emailAuthOnly = true;

  @GetMapping("/cas/authorize")
  @Operation(summary = "CAS login disabled", description = "WHUforum uses email/password login")
  @ApiResponse(
    responseCode = "403",
    description = "Email login only",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> casAuthorize(
    @RequestParam(value = "mockCampusId", required = false) String mockCampusId
  ) {
    return emailAuthOnlyResponse();
  }

  @PostMapping("/cas/callback")
  @Operation(summary = "CAS login disabled", description = "WHUforum uses email/password login")
  @ApiResponse(
    responseCode = "403",
    description = "Email login only",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> casCallback(@RequestBody Map<String, Object> req) {
    return emailAuthOnlyResponse();
  }

  @PostMapping("/register")
  @Operation(summary = "Register user", description = "Register a new user account")
  @ApiResponse(
    responseCode = "200",
    description = "Registration result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
    String email = normalizeRegistrationEmail(req.getEmail());
    if (!isWhuEmail(email)) {
      return ResponseEntity.badRequest().body(
        Map.of(
          "field",
          "email",
          "error",
          "Only @whu.edu.cn email registration is supported",
          "reason_code",
          "WHU_EMAIL_REQUIRED"
        )
      );
    }
    if (captchaEnabled && registerCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid captcha"));
    }
    if (req.getInviteToken() != null && !req.getInviteToken().isEmpty()) {
      InviteService.InviteValidateResult result = inviteService.validate(req.getInviteToken());
      if (!result.isValidate()) {
        return ResponseEntity.badRequest().body(Map.of("error", "邀请码使用次数过多"));
      }
      try {
        User user = userService.registerWithInvite(req.getUsername(), email, req.getPassword());
        inviteService.consume(req.getInviteToken(), user.getUsername());
        // 发送确认邮件
        userService.sendVerifyMail(user, VerifyType.REGISTER);
        return ResponseEntity.ok(
          Map.of(
            "token",
            jwtService.generateToken(user.getUsername()),
            "reason_code",
            "INVITE_APPROVED"
          )
        );
      } catch (EmailSendException e) {
        return emailSendFailureResponse(e);
      } catch (FieldException e) {
        return ResponseEntity.badRequest().body(
          Map.of("field", e.getField(), "error", e.getMessage())
        );
      }
    }
    User user = userService.register(
      req.getUsername(),
      email,
      req.getPassword(),
      "",
      registerModeService.getRegisterMode()
    );
    // 发送确认邮件
    try {
      userService.sendVerifyMail(user, VerifyType.REGISTER);
    } catch (EmailSendException e) {
      return emailSendFailureResponse(e);
    }
    if (!user.isApproved()) {
      notificationService.createRegisterRequestNotifications(user, user.getRegisterReason());
    }
    return ResponseEntity.ok(Map.of("message", "Activation email sent", "email", user.getEmail()));
  }

  @PostMapping("/verify")
  @Operation(summary = "Verify account", description = "Verify registration code")
  @ApiResponse(
    responseCode = "200",
    description = "Verification result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> verify(@RequestBody VerifyRequest req) {
    Optional<User> userOpt = userService.findByUsername(req.getUsername());
    if (userOpt.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
    }
    boolean ok = userService.verifyCode(userOpt.get(), req.getCode(), VerifyType.REGISTER);
    if (ok) {
      return verifiedResponse(userOpt.get());
    }
    return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
  }

  @PostMapping("/activate")
  @Operation(summary = "Activate account", description = "Activate registration by email link")
  @ApiResponse(
    responseCode = "200",
    description = "Activation result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> activate(@RequestBody ActivateRequest req) {
    Optional<User> userOpt = userService.activateRegisterToken(req == null ? null : req.getToken());
    if (userOpt.isEmpty()) {
      return ResponseEntity.badRequest().body(
        Map.of("error", "Invalid or expired activation token")
      );
    }
    return verifiedResponse(userOpt.get());
  }

  @PostMapping("/login")
  @Operation(summary = "Login", description = "Authenticate with registered WHU email and password")
  @ApiResponse(
    responseCode = "200",
    description = "Authentication result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    if (captchaEnabled && loginCaptchaEnabled && !captchaService.verify(req.getCaptcha())) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid captcha"));
    }
    String email = normalizeLoginEmail(req);
    if (!isWhuEmail(email)) {
      return ResponseEntity.badRequest().body(
        Map.of(
          "field",
          "email",
          "error",
          "Please login with a registered @whu.edu.cn email",
          "reason_code",
          "WHU_EMAIL_REQUIRED"
        )
      );
    }
    Optional<User> userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty() || !userService.matchesPassword(userOpt.get(), req.getPassword())) {
      return ResponseEntity.badRequest().body(
        Map.of("error", "Invalid credentials", "reason_code", "INVALID_CREDENTIALS")
      );
    }
    User user = userOpt.get();
    if (!user.isVerified()) {
      try {
        userService.sendVerifyMail(user, VerifyType.REGISTER);
      } catch (EmailSendException e) {
        return emailSendFailureResponse(e);
      }
      return ResponseEntity.badRequest().body(
        Map.of(
          "error",
          "User not verified",
          "reason_code",
          "NOT_VERIFIED",
          "user_name",
          user.getUsername()
        )
      );
    }
    if (
      RegisterMode.WHITELIST.equals(registerModeService.getRegisterMode()) && !user.isApproved()
    ) {
      if (user.getRegisterReason() != null && !user.getRegisterReason().isEmpty()) {
        return ResponseEntity.badRequest().body(
          Map.of("error", "Account awaiting approval", "reason_code", "IS_APPROVING")
        );
      }
      return ResponseEntity.badRequest().body(
        Map.of(
          "error",
          "Register reason not approved",
          "reason_code",
          "NOT_APPROVED",
          "token",
          jwtService.generateReasonToken(user.getUsername())
        )
      );
    }
    return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.getUsername())));
  }

  @PostMapping("/google")
  @Operation(summary = "Google login disabled", description = "WHUforum uses email/password login")
  @ApiResponse(
    responseCode = "403",
    description = "Email login only",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequest req) {
    return emailAuthOnlyResponse();
  }

  @PostMapping("/reason")
  @Operation(
    summary = "Submit register reason",
    description = "Submit registration reason for approval"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Submission result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> reason(@RequestBody MakeReasonRequest req) {
    String username = jwtService.validateAndGetSubjectForReason(req.getToken());
    Optional<User> userOpt = userService.findByUsername(username);
    if (userOpt.isEmpty()) {
      return ResponseEntity.badRequest().body(
        Map.of("error", "Invalid token, Please re-login", "reason_code", "INVALID_CREDENTIALS")
      );
    }

    if (req.getReason() == null || req.getReason().trim().length() <= 20) {
      return ResponseEntity.badRequest().body(
        Map.of("error", "Reason's length must longer than 20", "reason_code", "INVALID_CREDENTIALS")
      );
    }

    User user = userOpt.get();
    if (user.isApproved() || registerModeService.getRegisterMode() == RegisterMode.DIRECT) {
      return ResponseEntity.ok().body(Map.of("valid", true));
    }

    user = userService.updateReason(user.getUsername(), req.getReason());
    notificationService.createRegisterRequestNotifications(user, req.getReason());
    return ResponseEntity.ok().body(Map.of("valid", true));
  }

  @PostMapping("/github")
  @Operation(summary = "GitHub login disabled", description = "WHUforum uses email/password login")
  @ApiResponse(
    responseCode = "403",
    description = "Email login only",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> loginWithGithub(@RequestBody GithubLoginRequest req) {
    if (emailAuthOnly || whuMode) {
      return emailAuthOnlyResponse();
    }
    boolean viaInvite = req.getInviteToken() != null && !req.getInviteToken().isEmpty();
    InviteService.InviteValidateResult inviteValidateResult = inviteService.validate(
      req.getInviteToken()
    );
    if (viaInvite && !inviteValidateResult.isValidate()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid invite token"));
    }
    Optional<AuthResult> resultOpt = githubAuthService.authenticate(
      req.getCode(),
      registerModeService.getRegisterMode(),
      req.getRedirectUri(),
      viaInvite
    );
    if (resultOpt.isPresent()) {
      AuthResult result = resultOpt.get();
      if (viaInvite && result.isNewUser()) {
        inviteService.consume(
          req.getInviteToken(),
          inviteValidateResult.getInviteToken().getInviter().getUsername()
        );
        return ResponseEntity.ok(
          Map.of(
            "token",
            jwtService.generateToken(result.getUser().getUsername()),
            "reason_code",
            "INVITE_APPROVED"
          )
        );
      }
      if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
        return ResponseEntity.ok(
          Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
        );
      }
      if (!result.getUser().isApproved()) {
        if (
          result.getUser().getRegisterReason() != null &&
          !result.getUser().getRegisterReason().isEmpty()
        ) {
          // 已填写注册理由
          return ResponseEntity.badRequest().body(
            Map.of(
              "error",
              "Account awaiting approval",
              "reason_code",
              "IS_APPROVING",
              "token",
              jwtService.generateReasonToken(result.getUser().getUsername())
            )
          );
        }
        return ResponseEntity.badRequest().body(
          Map.of(
            "error",
            "Account awaiting approval",
            "reason_code",
            "NOT_APPROVED",
            "token",
            jwtService.generateReasonToken(result.getUser().getUsername())
          )
        );
      }

      return ResponseEntity.ok(
        Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
      );
    }
    return ResponseEntity.badRequest().body(
      Map.of("error", "Invalid github code", "reason_code", "INVALID_CREDENTIALS")
    );
  }

  @PostMapping("/discord")
  @Operation(summary = "Discord login disabled", description = "WHUforum uses email/password login")
  @ApiResponse(
    responseCode = "403",
    description = "Email login only",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> loginWithDiscord(@RequestBody DiscordLoginRequest req) {
    if (emailAuthOnly || whuMode) {
      return emailAuthOnlyResponse();
    }
    boolean viaInvite = req.getInviteToken() != null && !req.getInviteToken().isEmpty();
    InviteService.InviteValidateResult inviteValidateResult = inviteService.validate(
      req.getInviteToken()
    );
    if (viaInvite && !inviteValidateResult.isValidate()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid invite token"));
    }
    Optional<AuthResult> resultOpt = discordAuthService.authenticate(
      req.getCode(),
      registerModeService.getRegisterMode(),
      req.getRedirectUri(),
      viaInvite
    );
    if (resultOpt.isPresent()) {
      AuthResult result = resultOpt.get();
      if (viaInvite && result.isNewUser()) {
        inviteService.consume(
          req.getInviteToken(),
          inviteValidateResult.getInviteToken().getInviter().getUsername()
        );
        return ResponseEntity.ok(
          Map.of(
            "token",
            jwtService.generateToken(result.getUser().getUsername()),
            "reason_code",
            "INVITE_APPROVED"
          )
        );
      }
      if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
        return ResponseEntity.ok(
          Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
        );
      }
      if (!result.getUser().isApproved()) {
        if (
          result.getUser().getRegisterReason() != null &&
          !result.getUser().getRegisterReason().isEmpty()
        ) {
          return ResponseEntity.badRequest().body(
            Map.of(
              "error",
              "Account awaiting approval",
              "reason_code",
              "IS_APPROVING",
              "token",
              jwtService.generateReasonToken(result.getUser().getUsername())
            )
          );
        }
        return ResponseEntity.badRequest().body(
          Map.of(
            "error",
            "Account awaiting approval",
            "reason_code",
            "NOT_APPROVED",
            "token",
            jwtService.generateReasonToken(result.getUser().getUsername())
          )
        );
      }

      return ResponseEntity.ok(
        Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
      );
    }
    return ResponseEntity.badRequest().body(
      Map.of("error", "Invalid discord code", "reason_code", "INVALID_CREDENTIALS")
    );
  }

  @PostMapping("/twitter")
  @Operation(summary = "Twitter login disabled", description = "WHUforum uses email/password login")
  @ApiResponse(
    responseCode = "403",
    description = "Email login only",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> loginWithTwitter(@RequestBody TwitterLoginRequest req) {
    if (emailAuthOnly || whuMode) {
      return emailAuthOnlyResponse();
    }
    boolean viaInvite = req.getInviteToken() != null && !req.getInviteToken().isEmpty();
    InviteService.InviteValidateResult inviteValidateResult = inviteService.validate(
      req.getInviteToken()
    );
    if (viaInvite && !inviteValidateResult.isValidate()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid invite token"));
    }
    Optional<AuthResult> resultOpt = twitterAuthService.authenticate(
      req.getCode(),
      req.getCodeVerifier(),
      registerModeService.getRegisterMode(),
      req.getRedirectUri(),
      viaInvite
    );
    if (resultOpt.isPresent()) {
      AuthResult result = resultOpt.get();
      if (viaInvite && result.isNewUser()) {
        inviteService.consume(
          req.getInviteToken(),
          inviteValidateResult.getInviteToken().getInviter().getUsername()
        );
        return ResponseEntity.ok(
          Map.of(
            "token",
            jwtService.generateToken(result.getUser().getUsername()),
            "reason_code",
            "INVITE_APPROVED"
          )
        );
      }
      if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
        return ResponseEntity.ok(
          Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
        );
      }
      if (!result.getUser().isApproved()) {
        if (
          result.getUser().getRegisterReason() != null &&
          !result.getUser().getRegisterReason().isEmpty()
        ) {
          return ResponseEntity.badRequest().body(
            Map.of(
              "error",
              "Account awaiting approval",
              "reason_code",
              "IS_APPROVING",
              "token",
              jwtService.generateReasonToken(result.getUser().getUsername())
            )
          );
        }
        return ResponseEntity.badRequest().body(
          Map.of(
            "error",
            "Account awaiting approval",
            "reason_code",
            "NOT_APPROVED",
            "token",
            jwtService.generateReasonToken(result.getUser().getUsername())
          )
        );
      }

      return ResponseEntity.ok(
        Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
      );
    }
    return ResponseEntity.badRequest().body(
      Map.of("error", "Invalid twitter code", "reason_code", "INVALID_CREDENTIALS")
    );
  }

  @PostMapping("/telegram")
  @Operation(
    summary = "Telegram login disabled",
    description = "WHUforum uses email/password login"
  )
  @ApiResponse(
    responseCode = "403",
    description = "Email login only",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> loginWithTelegram(@RequestBody TelegramLoginRequest req) {
    if (emailAuthOnly || whuMode) {
      return emailAuthOnlyResponse();
    }
    boolean viaInvite = req.getInviteToken() != null && !req.getInviteToken().isEmpty();
    InviteService.InviteValidateResult inviteValidateResult = inviteService.validate(
      req.getInviteToken()
    );
    if (viaInvite && !inviteValidateResult.isValidate()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid invite token"));
    }
    Optional<AuthResult> resultOpt = telegramAuthService.authenticate(
      req,
      registerModeService.getRegisterMode(),
      viaInvite
    );
    if (resultOpt.isPresent()) {
      AuthResult result = resultOpt.get();
      if (viaInvite && result.isNewUser()) {
        inviteService.consume(
          req.getInviteToken(),
          inviteValidateResult.getInviteToken().getInviter().getUsername()
        );
        return ResponseEntity.ok(
          Map.of(
            "token",
            jwtService.generateToken(result.getUser().getUsername()),
            "reason_code",
            "INVITE_APPROVED"
          )
        );
      }
      if (RegisterMode.DIRECT.equals(registerModeService.getRegisterMode())) {
        return ResponseEntity.ok(
          Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
        );
      }
      if (!result.getUser().isApproved()) {
        if (
          result.getUser().getRegisterReason() != null &&
          !result.getUser().getRegisterReason().isEmpty()
        ) {
          return ResponseEntity.badRequest().body(
            Map.of(
              "error",
              "Account awaiting approval",
              "reason_code",
              "IS_APPROVING",
              "token",
              jwtService.generateReasonToken(result.getUser().getUsername())
            )
          );
        }
        return ResponseEntity.badRequest().body(
          Map.of(
            "error",
            "Account awaiting approval",
            "reason_code",
            "NOT_APPROVED",
            "token",
            jwtService.generateReasonToken(result.getUser().getUsername())
          )
        );
      }
      return ResponseEntity.ok(
        Map.of("token", jwtService.generateToken(result.getUser().getUsername()))
      );
    }
    return ResponseEntity.badRequest().body(
      Map.of("error", "Invalid telegram data", "reason_code", "INVALID_CREDENTIALS")
    );
  }

  @GetMapping("/check")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Check token", description = "Validate JWT token")
  @ApiResponse(
    responseCode = "200",
    description = "Token valid",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> checkToken() {
    return ResponseEntity.ok(Map.of("valid", true));
  }

  @PostMapping("/forgot/send")
  @Operation(summary = "Send reset link", description = "Send password reset link by email")
  @ApiResponse(
    responseCode = "200",
    description = "Sending result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> sendReset(@RequestBody ForgotPasswordRequest req) {
    String email = normalizeRegistrationEmail(req.getEmail());
    if (!isWhuEmail(email)) {
      return ResponseEntity.badRequest().body(
        Map.of(
          "field",
          "email",
          "error",
          "Please use your @whu.edu.cn email",
          "reason_code",
          "WHU_EMAIL_REQUIRED"
        )
      );
    }
    Optional<User> userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
    }
    try {
      User user = userOpt.get();
      userService.sendPasswordResetMail(user, jwtService.generateResetToken(user.getUsername()));
    } catch (EmailSendException e) {
      return emailSendFailureResponse(e);
    }
    return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
  }

  @PostMapping("/forgot/verify")
  @Operation(summary = "Verify reset code", description = "Verify password reset code")
  @ApiResponse(
    responseCode = "200",
    description = "Verification result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> verifyReset(@RequestBody VerifyForgotRequest req) {
    String email = normalizeRegistrationEmail(req.getEmail());
    Optional<User> userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
    }
    boolean ok = userService.verifyCode(userOpt.get(), req.getCode(), VerifyType.RESET_PASSWORD);
    if (ok) {
      String username = userOpt.get().getUsername();
      return ResponseEntity.ok(Map.of("token", jwtService.generateResetToken(username)));
    }
    return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
  }

  @PostMapping("/forgot/reset")
  @Operation(summary = "Reset password", description = "Reset user password after verification")
  @ApiResponse(
    responseCode = "200",
    description = "Reset result",
    content = @Content(schema = @Schema(implementation = Map.class))
  )
  public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
    String username;
    try {
      username = jwtService.validateAndGetSubjectForReset(req.getToken());
    } catch (JwtException | IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(
        Map.of("error", "Invalid or expired reset token", "reason_code", "INVALID_RESET_TOKEN")
      );
    }
    try {
      userService.updatePassword(username, req.getPassword());
      return ResponseEntity.ok(Map.of("message", "Password updated"));
    } catch (FieldException e) {
      return ResponseEntity.badRequest().body(
        Map.of("field", e.getField(), "error", e.getMessage())
      );
    }
  }

  private ResponseEntity<?> emailAuthOnlyResponse() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
      Map.of(
        "error",
        "WHUforum only supports @whu.edu.cn registration and registered email/password login",
        "reason_code",
        "EMAIL_AUTH_ONLY"
      )
    );
  }

  private ResponseEntity<?> verifiedResponse(User user) {
    if (user.isApproved()) {
      return ResponseEntity.ok(
        Map.of(
          "message",
          "Verified and isApproved",
          "reason_code",
          "VERIFIED_AND_APPROVED",
          "token",
          jwtService.generateToken(user.getUsername())
        )
      );
    }
    return ResponseEntity.ok(
      Map.of(
        "message",
        "Verified",
        "reason_code",
        "VERIFIED",
        "token",
        jwtService.generateReasonToken(user.getUsername())
      )
    );
  }

  private ResponseEntity<?> emailSendFailureResponse(EmailSendException e) {
    log.warn("Email send failed: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
      Map.of("error", "邮件发送失败，请稍后再试或联系管理员。", "reason_code", "EMAIL_SEND_FAILED")
    );
  }

  private String normalizeLoginEmail(LoginRequest req) {
    if (req == null) {
      return "";
    }
    String email = req.getEmail();
    if (email == null || email.isBlank()) {
      email = req.getUsername();
    }
    return normalizeRegistrationEmail(email);
  }

  private String normalizeRegistrationEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private boolean isWhuEmail(String email) {
    return email != null && email.endsWith("@whu.edu.cn");
  }

  private String normalizeOptionalToken(String token) {
    return token == null ? "" : token.trim();
  }

  // DTO classes moved to com.openisle.dto package
}
