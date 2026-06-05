package com.openisle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openisle.config.CachingConfig;
import com.openisle.exception.FieldException;
import com.openisle.model.CampusPersonType;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UserServiceTest {

  @Test
  void deleteAccountAnonymizesUserAndInvalidatesPassword() {
    UserRepository userRepository = mock(UserRepository.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    ValueOperations valueOperations = mock(ValueOperations.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    UserService userService = new UserService(
      userRepository,
      mock(PasswordValidator.class),
      mock(UsernameValidator.class),
      imageUploader,
      mock(AvatarGenerator.class),
      redisTemplate,
      mock(EmailSender.class),
      searchIndexEventPublisher,
      mock(CampusIdentityService.class)
    );
    User user = new User();
    user.setId(42L);
    user.setUsername("alice");
    user.setEmail("alice@whu.edu.cn");
    user.setPassword(encoder.encode("secret"));
    user.setAvatar("https://cdn.example.com/avatar.png");
    user.setIntroduction("hello");
    user.setRegisterReason("reason");
    user.setCasSubject("cas");
    user.setCampusIdHash("hash");
    user.setEncryptedCampusId("encrypted");
    user.setCampusPersonType(CampusPersonType.STUDENT);
    user.setDepartment("WHU");
    user.setCampusVerified(true);

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(any())).thenReturn("activation-token");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    userService.deleteAccount("alice", "secret");

    assertThat(user.getUsername()).isEqualTo("deleted-user-42");
    assertThat(user.getEmail()).isEqualTo("deleted-user-42@deleted.openisle.local");
    assertThat(encoder.matches("secret", user.getPassword())).isFalse();
    assertThat(user.isVerified()).isFalse();
    assertThat(user.isApproved()).isFalse();
    assertThat(user.getAvatar()).isNull();
    assertThat(user.getIntroduction()).isEmpty();
    assertThat(user.getRegisterReason()).isNull();
    assertThat(user.getCasSubject()).isNull();
    assertThat(user.getCampusIdHash()).isNull();
    assertThat(user.getEncryptedCampusId()).isNull();
    assertThat(user.getCampusPersonType()).isEqualTo(CampusPersonType.UNKNOWN);
    assertThat(user.getDepartment()).isNull();
    assertThat(user.isCampusVerified()).isFalse();
    verify(imageUploader).removeReferences(Set.of("https://cdn.example.com/avatar.png"));
    verify(redisTemplate).delete(CachingConfig.VERIFY_CACHE_NAME + ":register:mail_cooldown:alice");
    verify(redisTemplate).delete(
      CachingConfig.VERIFY_CACHE_NAME + ":reset_password:mail_cooldown:alice"
    );
    verify(searchIndexEventPublisher).publishUserSaved(user);
  }

  @Test
  void deleteAccountRejectsWrongPassword() {
    UserRepository userRepository = mock(UserRepository.class);
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    UserService userService = new UserService(
      userRepository,
      mock(PasswordValidator.class),
      mock(UsernameValidator.class),
      mock(ImageUploader.class),
      mock(AvatarGenerator.class),
      redisTemplate,
      mock(EmailSender.class),
      mock(SearchIndexEventPublisher.class),
      mock(CampusIdentityService.class)
    );
    User user = new User();
    user.setUsername("alice");
    user.setPassword(encoder.encode("secret"));
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> userService.deleteAccount("alice", "bad"))
      .isInstanceOf(FieldException.class)
      .hasMessage("Invalid password");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void sendPasswordResetMailNormalizesLocalhostHttpsAndSkipsCooldown() {
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    ValueOperations valueOperations = mock(ValueOperations.class);
    EmailSender emailSender = mock(EmailSender.class);
    UserService userService = new UserService(
      mock(UserRepository.class),
      mock(PasswordValidator.class),
      mock(UsernameValidator.class),
      mock(ImageUploader.class),
      mock(AvatarGenerator.class),
      redisTemplate,
      emailSender,
      mock(SearchIndexEventPublisher.class),
      mock(CampusIdentityService.class)
    );
    ReflectionTestUtils.setField(userService, "websiteUrl", "https://localhost:3000");
    User user = new User();
    user.setUsername("alice");
    user.setEmail("Alice@WHU.EDU.CN");
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(any())).thenReturn(null, "alice@whu.edu.cn");

    userService.sendPasswordResetMail(user, "reset-token");
    userService.sendPasswordResetMail(user, "second-token");

    ArgumentCaptor<Map<String, String>> templateCaptor = ArgumentCaptor.forClass(Map.class);
    verify(emailSender).sendEmail(
      eq("Alice@WHU.EDU.CN"),
      eq("珞珈论坛密码重置"),
      any(),
      templateCaptor.capture()
    );
    assertThat(templateCaptor.getValue().get("resetUrl")).isEqualTo(
      "http://localhost:3000/reset-password?token=reset-token"
    );
    verify(valueOperations).set(
      eq(CachingConfig.VERIFY_CACHE_NAME + ":reset_password:mail_cooldown:alice"),
      eq("alice@whu.edu.cn"),
      eq(60L),
      eq(TimeUnit.SECONDS)
    );
  }
}
