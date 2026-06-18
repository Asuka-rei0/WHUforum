package com.openisle.service;

import com.openisle.config.CachingConfig;
import com.openisle.exception.EmailSendException;
import com.openisle.exception.FieldException;
import com.openisle.model.CampusPersonType;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import com.openisle.service.AvatarGenerator;
import com.openisle.service.PasswordValidator;
import com.openisle.service.UsernameValidator;
import com.openisle.util.VerifyType;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private static final SecureRandom VERIFY_CODE_RANDOM = new SecureRandom();
  private static final int ACTIVATION_TOKEN_BYTES = 32;
  private static final long EMAIL_RESEND_COOLDOWN_SECONDS = 60;

  private final UserRepository userRepository;
  private final PasswordValidator passwordValidator;
  private final UsernameValidator usernameValidator;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final ImageUploader imageUploader;
  private final AvatarGenerator avatarGenerator;

  private final RedisTemplate redisTemplate;

  private final EmailSender emailService;
  private final SearchIndexEventPublisher searchIndexEventPublisher;
  private final CampusIdentityService campusIdentityService;

  @Value("${app.website-url:http://localhost:3000}")
  private String websiteUrl;

  @Value("${app.email.verification-token-expiration-minutes:1440}")
  private long verificationTokenExpirationMinutes;

  public record CampusUserUpsertResult(User user, boolean created) {}

  public User register(
    String username,
    String email,
    String password,
    String reason,
    com.openisle.model.RegisterMode mode
  ) {
    usernameValidator.validate(username);
    passwordValidator.validate(password);
    // ── 先按用户名查 ──────────────────────────────────────────
    Optional<User> byUsername = userRepository.findByUsername(username);
    if (byUsername.isPresent()) {
      User u = byUsername.get();
      if (u.isVerified()) {
        // 已验证 → 直接拒绝
        throw new FieldException("username", "User name already exists");
      }
      // 未验证 → 允许“重注册”：覆盖必要字段并重新发验证码
      u.setEmail(email); // 若不允许改邮箱可去掉
      u.setPassword(passwordEncoder.encode(password));
      //            u.setVerificationCode(genCode());
      u.setRegisterReason(reason);
      u.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
      User saved = userRepository.save(u);
      searchIndexEventPublisher.publishUserSaved(saved);
      return saved;
    }

    // ── 再按邮箱查 ───────────────────────────────────────────
    Optional<User> byEmail = userRepository.findByEmail(email);
    if (byEmail.isPresent()) {
      User u = byEmail.get();
      if (u.isVerified()) {
        // 已验证 → 直接拒绝
        throw new FieldException("email", "User email already exists");
      }
      // 未验证 → 允许“重注册”
      u.setUsername(username); // 若不允许改用户名可去掉
      u.setPassword(passwordEncoder.encode(password));
      //            u.setVerificationCode(genCode());
      u.setRegisterReason(reason);
      u.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
      User saved = userRepository.save(u);
      searchIndexEventPublisher.publishUserSaved(saved);
      return saved;
    }

    // ── 完全新用户 ───────────────────────────────────────────
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(Role.USER);
    user.setVerified(false);
    //        user.setVerificationCode(genCode());
    user.setAvatar(avatarGenerator.generate(username));
    user.setRegisterReason(reason);
    user.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
    User saved = userRepository.save(user);
    searchIndexEventPublisher.publishUserSaved(saved);
    return saved;
  }

  public User registerWithInvite(String username, String email, String password) {
    User user = register(username, email, password, "", com.openisle.model.RegisterMode.DIRECT);
    user.setVerified(true);
    //        user.setVerificationCode(genCode());
    User saved = userRepository.save(user);
    searchIndexEventPublisher.publishUserSaved(saved);
    return saved;
  }

  public User upsertCampusUser(WhuCasProfile profile) {
    return upsertCampusUserWithResult(profile).user();
  }

  public CampusUserUpsertResult upsertCampusUserWithResult(WhuCasProfile profile) {
    String campusIdHash = campusIdentityService.hashCampusId(profile.campusId());
    Optional<User> existing = userRepository.findByCasSubject(profile.subject());
    if (existing.isEmpty() && campusIdHash != null) {
      existing = userRepository.findByCampusIdHash(campusIdHash);
    }

    boolean created = existing.isEmpty();
    User user = existing.orElseGet(User::new);
    String username = normalizeUsername(profile.displayName(), profile.campusId());
    if (user.getId() == null) {
      username = nextAvailableUsername(username);
      user.setUsername(username);
      user.setEmail(
        profile.email() != null && !profile.email().isBlank()
          ? profile.email()
          : username + "@whu.edu.cn"
      );
      user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
      user.setRole(Role.USER);
      user.setAvatar(avatarGenerator.generate(username));
      user.setApproved(true);
      user.setVerified(true);
    }

    user.setCasSubject(profile.subject());
    user.setCampusIdHash(campusIdHash);
    user.setEncryptedCampusId(campusIdentityService.encryptCampusId(profile.campusId()));
    user.setCampusPersonType(
      profile.personType() != null ? profile.personType() : CampusPersonType.UNKNOWN
    );
    user.setDepartment(profile.department());
    user.setCampusVerified(true);
    User saved = userRepository.save(user);
    searchIndexEventPublisher.publishUserSaved(saved);
    return new CampusUserUpsertResult(saved, created);
  }

  private String normalizeUsername(String displayName, String campusId) {
    String base = displayName == null || displayName.isBlank()
      ? "whu" + (campusId == null ? "user" : campusId)
      : displayName;
    return base
      .trim()
      .toLowerCase(java.util.Locale.ROOT)
      .replaceAll("[^a-z0-9_\\-\\u4e00-\\u9fa5]", "_");
  }

  private String nextAvailableUsername(String base) {
    String candidate = base;
    int suffix = 1;
    while (userRepository.findByUsername(candidate).isPresent()) {
      candidate = base + suffix;
      suffix++;
    }
    return candidate;
  }

  private String genCode() {
    return String.format("%06d", VERIFY_CODE_RANDOM.nextInt(1000000));
  }

  private String genActivationToken() {
    byte[] bytes = new byte[ACTIVATION_TOKEN_BYTES];
    VERIFY_CODE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * 将验证码存入缓存，并发送邮件
   * @param user
   */
  public void sendVerifyMail(User user, VerifyType verifyType) {
    String code = genCode();
    String key = verifyCodeKey(user.getUsername(), verifyType);
    String subject;
    String content;
    Map<String, String> templateData = new HashMap<>();
    templateData.put("username", user.getUsername());
    templateData.put("email", user.getEmail());
    templateData.put("code", code);
    templateData.put("token", code);
    templateData.put("activateUrl", "");
    templateData.put("resetUrl", "");
    // 注册类型
    if (verifyType.equals(VerifyType.REGISTER)) {
      String cooldownKey = registerMailCooldownKey(user.getUsername());
      if (isMailCooldownActive(cooldownKey, user.getEmail())) {
        return;
      }
      subject = "珞珈论坛邮箱验证";
      String activationToken = genActivationToken();
      String activationUrl = activationUrl(activationToken);
      content =
        "你好 " +
        user.getUsername() +
        "，请点击以下链接激活你的珞珈论坛账号：" +
        activationUrl +
        "。如果链接无法打开，也可以使用验证码 " +
        code +
        " 完成验证。";
      templateData.put("_purpose", "register");
      templateData.put("token", activationToken);
      templateData.put("activateUrl", activationUrl);
      cacheRegisterVerification(user.getUsername(), code, activationToken);
      try {
        emailService.sendEmail(user.getEmail(), subject, content, templateData);
        cacheMailCooldown(cooldownKey, user.getEmail());
      } catch (EmailSendException e) {
        clearRegisterVerification(user.getUsername(), activationToken, e);
        throw e;
      }
      return;
    } else {
      // 重置密码
      subject = "珞珈论坛密码重置";
      content = "你的珞珈论坛验证码是：" + code + "，有效期为 5 分钟。";
      templateData.put("_purpose", "reset_password");
    }

    redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES); // 五分钟后验证码过期
    try {
      emailService.sendEmail(user.getEmail(), subject, content, templateData);
    } catch (EmailSendException e) {
      clearVerifyCode(key, e);
      throw e;
    }
  }

  public void sendPasswordResetMail(User user, String resetToken) {
    String normalizedToken = StringUtils.trimToEmpty(resetToken);
    if (normalizedToken.isBlank()) {
      throw new EmailSendException("Password reset token is empty");
    }
    String cooldownKey = resetMailCooldownKey(user.getUsername());
    if (isMailCooldownActive(cooldownKey, user.getEmail())) {
      return;
    }

    String resetUrl = resetUrl(normalizedToken);
    String subject = "珞珈论坛密码重置";
    String content =
      "你好 " +
      user.getUsername() +
      "，请点击以下链接重置你的珞珈论坛密码：" +
      resetUrl +
      "。如果不是你本人操作，请忽略这封邮件。";
    Map<String, String> templateData = new HashMap<>();
    templateData.put("_purpose", "reset_password");
    templateData.put("username", user.getUsername());
    templateData.put("email", user.getEmail());
    templateData.put("token", normalizedToken);
    templateData.put("resetUrl", resetUrl);
    templateData.put("activateUrl", "");

    emailService.sendEmail(user.getEmail(), subject, content, templateData);
    cacheMailCooldown(cooldownKey, user.getEmail());
  }

  public Optional<User> activateRegisterToken(String token) {
    String normalizedToken = StringUtils.trimToEmpty(token);
    if (normalizedToken.isBlank()) {
      return Optional.empty();
    }
    String tokenKey = registerTokenKey(normalizedToken);
    Object cachedUsername = redisTemplate.opsForValue().get(tokenKey);
    if (!(cachedUsername instanceof String username) || username.isBlank()) {
      return Optional.empty();
    }

    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      redisTemplate.delete(tokenKey);
      redisTemplate.delete(registerTokenByUserKey(username));
      return Optional.empty();
    }

    User user = userOpt.get();
    if (!user.isVerified()) {
      user.setVerified(true);
      user = userRepository.save(user);
      searchIndexEventPublisher.publishUserSaved(user);
    }
    redisTemplate.delete(tokenKey);
    redisTemplate.delete(registerTokenByUserKey(username));
    redisTemplate.delete(verifyCodeKey(username, VerifyType.REGISTER));
    return Optional.of(user);
  }

  /**
   * 验证code是否正确
   * @param user
   * @param code
   * @param verifyType
   * @return
   */
  public boolean verifyCode(User user, String code, VerifyType verifyType) {
    // 生成key
    String key1 = VerifyType.REGISTER.equals(verifyType)
      ? ":register:code:"
      : ":reset_password:code:";
    String key = CachingConfig.VERIFY_CACHE_NAME + key1 + user.getUsername();
    // 这里不能使用getAndDelete,需要6.x版本
    String cachedCode = (String) redisTemplate.opsForValue().get(key);
    // 如果校验code过期或者不存在
    // 或者校验code不一致
    if (Objects.isNull(cachedCode) || !cachedCode.equals(code)) {
      return false;
    }
    // 注册模式需要设置已经确认
    if (VerifyType.REGISTER.equals(verifyType)) {
      user.setVerified(true);
      userRepository.save(user);
    }
    // 走到这里说明验证成功删除验证码
    redisTemplate.delete(key);
    return true;
  }

  private void cacheRegisterVerification(String username, String code, String activationToken) {
    String tokenByUserKey = registerTokenByUserKey(username);
    Object previousToken = redisTemplate.opsForValue().get(tokenByUserKey);
    if (previousToken instanceof String previousTokenValue) {
      redisTemplate.delete(registerTokenKey(previousTokenValue));
    }

    redisTemplate
      .opsForValue()
      .set(verifyCodeKey(username, VerifyType.REGISTER), code, 5, TimeUnit.MINUTES);
    redisTemplate
      .opsForValue()
      .set(
        registerTokenKey(activationToken),
        username,
        verificationTokenExpirationMinutes,
        TimeUnit.MINUTES
      );
    redisTemplate
      .opsForValue()
      .set(tokenByUserKey, activationToken, verificationTokenExpirationMinutes, TimeUnit.MINUTES);
  }

  private void clearRegisterVerification(
    String username,
    String activationToken,
    EmailSendException emailException
  ) {
    try {
      String tokenByUserKey = registerTokenByUserKey(username);
      Object cachedToken = redisTemplate.opsForValue().get(tokenByUserKey);
      redisTemplate.delete(registerTokenKey(activationToken));
      if (activationToken.equals(cachedToken)) {
        redisTemplate.delete(verifyCodeKey(username, VerifyType.REGISTER));
        redisTemplate.delete(tokenByUserKey);
      }
    } catch (RuntimeException cleanupException) {
      emailException.addSuppressed(cleanupException);
    }
  }

  private void clearVerifyCode(String key, EmailSendException emailException) {
    try {
      redisTemplate.delete(key);
    } catch (RuntimeException cleanupException) {
      emailException.addSuppressed(cleanupException);
    }
  }

  private String activationUrl(String activationToken) {
    return (
      normalizedWebsiteUrl() +
      "/activate?token=" +
      URLEncoder.encode(activationToken, StandardCharsets.UTF_8)
    );
  }

  private String resetUrl(String resetToken) {
    return (
      normalizedWebsiteUrl() +
      "/reset-password?token=" +
      URLEncoder.encode(resetToken, StandardCharsets.UTF_8)
    );
  }

  private String normalizedWebsiteUrl() {
    String baseUrl = StringUtils.stripEnd(
      StringUtils.defaultIfBlank(websiteUrl, "http://localhost:3000"),
      "/"
    );
    try {
      URI uri = URI.create(baseUrl);
      if ("https".equalsIgnoreCase(uri.getScheme()) && isLocalHost(uri.getHost())) {
        return StringUtils.stripEnd(
          new URI(
            "http",
            uri.getUserInfo(),
            uri.getHost(),
            uri.getPort(),
            uri.getPath(),
            uri.getQuery(),
            uri.getFragment()
          ).toString(),
          "/"
        );
      }
    } catch (IllegalArgumentException | URISyntaxException ignored) {
      return baseUrl;
    }
    return baseUrl;
  }

  private boolean isLocalHost(String host) {
    if (host == null) {
      return false;
    }
    String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
    return (
      "localhost".equals(normalizedHost) ||
      "127.0.0.1".equals(normalizedHost) ||
      "::1".equals(normalizedHost) ||
      "[::1]".equals(normalizedHost)
    );
  }

  private boolean isMailCooldownActive(String key, String email) {
    Object cachedEmail = redisTemplate.opsForValue().get(key);
    return (
      cachedEmail instanceof String sentTo && normalizeEmail(sentTo).equals(normalizeEmail(email))
    );
  }

  private void cacheMailCooldown(String key, String email) {
    redisTemplate
      .opsForValue()
      .set(key, normalizeEmail(email), EMAIL_RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
  }

  private String normalizeEmail(String email) {
    return StringUtils.trimToEmpty(email).toLowerCase(Locale.ROOT);
  }

  private String verifyCodeKey(String username, VerifyType verifyType) {
    String keyPart = VerifyType.REGISTER.equals(verifyType)
      ? ":register:code:"
      : ":reset_password:code:";
    return CachingConfig.VERIFY_CACHE_NAME + keyPart + username;
  }

  private String registerTokenKey(String token) {
    return CachingConfig.VERIFY_CACHE_NAME + ":register:token:" + token;
  }

  private String registerTokenByUserKey(String username) {
    return CachingConfig.VERIFY_CACHE_NAME + ":register:token_by_user:" + username;
  }

  private String registerMailCooldownKey(String username) {
    return CachingConfig.VERIFY_CACHE_NAME + ":register:mail_cooldown:" + username;
  }

  private String resetMailCooldownKey(String username) {
    return CachingConfig.VERIFY_CACHE_NAME + ":reset_password:mail_cooldown:" + username;
  }

  public Optional<User> authenticate(String username, String password) {
    return userRepository
      .findByUsername(username)
      .filter(User::isVerified)
      .filter(User::isApproved)
      .filter(user -> passwordEncoder.matches(password, user.getPassword()));
  }

  public boolean matchesPassword(User user, String rawPassword) {
    return passwordEncoder.matches(rawPassword, user.getPassword());
  }

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  public Optional<User> findByIdentifier(String identifier) {
    if (identifier.matches("\\d+")) {
      return userRepository.findById(Long.parseLong(identifier));
    }
    return userRepository.findByUsername(identifier);
  }

  public User updateAvatar(String username, String avatarUrl) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    String old = user.getAvatar();
    user.setAvatar(avatarUrl);
    User saved = userRepository.save(user);
    if (old != null && !old.equals(avatarUrl)) {
      imageUploader.removeReferences(java.util.Set.of(old));
    }
    if (avatarUrl != null) {
      imageUploader.addReferences(java.util.Set.of(avatarUrl));
    }
    return saved;
  }

  public User updateReason(String username, String reason) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    user.setRegisterReason(reason);
    User saved = userRepository.save(user);
    searchIndexEventPublisher.publishUserSaved(saved);
    return saved;
  }

  public User updateProfile(
    String currentUsername,
    String newUsername,
    String introduction,
    Boolean showCampusIdentity,
    Boolean showDepartment
  ) {
    User user = userRepository
      .findByUsername(currentUsername)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (newUsername != null && !newUsername.equals(currentUsername)) {
      usernameValidator.validate(newUsername);
      userRepository
        .findByUsername(newUsername)
        .ifPresent(u -> {
          throw new FieldException("username", "User name already exists");
        });
      user.setUsername(newUsername);
    }
    if (introduction != null) {
      user.setIntroduction(introduction);
    }
    if (showCampusIdentity != null) {
      user.setShowCampusIdentity(showCampusIdentity);
    }
    if (showDepartment != null) {
      user.setShowDepartment(showDepartment);
    }
    return userRepository.save(user);
  }

  public User updateProfile(String currentUsername, String newUsername, String introduction) {
    return updateProfile(currentUsername, newUsername, introduction, null, null);
  }

  public User updatePassword(String username, String newPassword) {
    passwordValidator.validate(newPassword);
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    user.setPassword(passwordEncoder.encode(newPassword));
    return userRepository.save(user);
  }

  @Transactional
  public void deleteAccount(String username, String password) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (StringUtils.isBlank(password) || !passwordEncoder.matches(password, user.getPassword())) {
      throw new FieldException("password", "Invalid password");
    }

    String deletedIdentity = "deleted-user-" + user.getId();
    String oldAvatar = user.getAvatar();
    user.setUsername(deletedIdentity);
    user.setEmail(deletedIdentity + "@deleted.openisle.local");
    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
    user.setVerified(false);
    user.setApproved(false);
    user.setAvatar(null);
    user.setIntroduction("");
    user.setRegisterReason(null);
    user.setCasSubject(null);
    user.setCampusIdHash(null);
    user.setEncryptedCampusId(null);
    user.setCampusPersonType(CampusPersonType.UNKNOWN);
    user.setDepartment(null);
    user.setCampusVerified(false);
    user.setShowCampusIdentity(true);
    user.setShowDepartment(true);
    user.setNotificationSiteEnabled(true);
    user.setNotificationEmailEnabled(true);
    user.setNotificationPushEnabled(true);
    user.setNotificationDigestFrequency("NONE");
    user.setDisplayMedal(null);
    user.getDisabledNotificationTypes().clear();
    user.getDisabledEmailNotificationTypes().clear();

    Object activationToken = redisTemplate.opsForValue().get(registerTokenByUserKey(username));
    User saved = userRepository.save(user);
    redisTemplate.delete(verifyCodeKey(username, VerifyType.REGISTER));
    redisTemplate.delete(verifyCodeKey(username, VerifyType.RESET_PASSWORD));
    redisTemplate.delete(registerTokenByUserKey(username));
    redisTemplate.delete(registerMailCooldownKey(username));
    redisTemplate.delete(resetMailCooldownKey(username));
    if (activationToken instanceof String token) {
      redisTemplate.delete(registerTokenKey(token));
    }
    if (oldAvatar != null) {
      imageUploader.removeReferences(java.util.Set.of(oldAvatar));
    }
    searchIndexEventPublisher.publishUserSaved(saved);
  }

  /**
   * Get all administrator accounts.
   */
  public java.util.List<User> getAdmins() {
    return userRepository.findByRole(Role.ADMIN);
  }
}
