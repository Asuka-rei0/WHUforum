package com.openisle.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class SystemUserInitializerTest {

  private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final SystemUserInitializer initializer = new SystemUserInitializer(
    userRepository,
    passwordEncoder
  );

  @BeforeEach
  void enableAdminInitializer() {
    ReflectionTestUtils.setField(initializer, "initAdmin", true);
  }

  @Test
  void createsDefaultAdminWhenMissing() {
    when(userRepository.findByUsername("system")).thenReturn(existingSystemUser());
    when(userRepository.findByEmail("admin@whu.edu.cn")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    initializer.run();

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedAdmin = userCaptor.getValue();
    assertThat(savedAdmin.getUsername()).isEqualTo("admin");
    assertThat(savedAdmin.getEmail()).isEqualTo("admin@whu.edu.cn");
    assertThat(savedAdmin.getRole()).isEqualTo(Role.ADMIN);
    assertThat(savedAdmin.isVerified()).isTrue();
    assertThat(savedAdmin.isApproved()).isTrue();
    assertThat(passwordEncoder.matches("123456", savedAdmin.getPassword())).isTrue();
  }

  @Test
  void repairsExistingAdminWithoutChangingPassword() {
    User admin = new User();
    admin.setUsername("admin");
    admin.setEmail("admin@whu.edu.cn");
    admin.setPassword(passwordEncoder.encode("custom-password"));
    admin.setRole(Role.USER);
    admin.setVerified(false);
    admin.setApproved(false);

    when(userRepository.findByUsername("system")).thenReturn(existingSystemUser());
    when(userRepository.findByEmail("admin@whu.edu.cn")).thenReturn(Optional.of(admin));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    initializer.run();

    assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    assertThat(admin.isVerified()).isTrue();
    assertThat(admin.isApproved()).isTrue();
    assertThat(passwordEncoder.matches("custom-password", admin.getPassword())).isTrue();
  }

  @Test
  void doesNotPromoteExistingNonAdminUsername() {
    User regularUser = new User();
    regularUser.setUsername("admin");
    regularUser.setEmail("regular@whu.edu.cn");
    regularUser.setPassword(passwordEncoder.encode("custom-password"));
    regularUser.setRole(Role.USER);

    when(userRepository.findByUsername("system")).thenReturn(existingSystemUser());
    when(userRepository.findByEmail("admin@whu.edu.cn")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(regularUser));

    initializer.run();

    assertThat(regularUser.getEmail()).isEqualTo("regular@whu.edu.cn");
    assertThat(regularUser.getRole()).isEqualTo(Role.USER);
  }

  private Optional<User> existingSystemUser() {
    User system = new User();
    system.setUsername("system");
    system.setEmail("system@openisle.local");
    return Optional.of(system);
  }
}
