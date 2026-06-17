package com.openisle.config;

import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensure a dedicated "system" user exists for internal operations.
 */
@Component
@RequiredArgsConstructor
public class SystemUserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.whu.init-admin:false}")
  private boolean initAdmin = false;

  @Value("${app.whu.admin-email:admin@whu.edu.cn}")
  private String adminEmail = "admin@whu.edu.cn";

  @Value("${app.whu.admin-password:123456}")
  private String adminPassword = "123456";

  @Override
  public void run(String... args) {
    userRepository
      .findByUsername("system")
      .orElseGet(() -> {
        User system = new User();
        system.setUsername("system");
        system.setEmail("system@openisle.local");
        // todo(tim): raw password 采用环境变量
        system.setPassword(passwordEncoder.encode("system"));
        system.setRole(Role.USER);
        system.setVerified(true);
        system.setApproved(true);
        system.setAvatar(
          "https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/image.png"
        );
        return userRepository.save(system);
      });

    if (initAdmin) {
      ensureAdminUser();
    }
  }

  private void ensureAdminUser() {
    String normalizedEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
    if (normalizedEmail.isBlank()) {
      return;
    }
    Optional<User> byEmail = userRepository.findByEmail(normalizedEmail);
    if (byEmail.isPresent()) {
      ensureAdminPrivileges(byEmail.get());
      return;
    }

    Optional<User> byUsername = userRepository.findByUsername("admin");
    if (byUsername.isPresent()) {
      User admin = byUsername.get();
      if (admin.getRole() == Role.ADMIN) {
        admin.setEmail(normalizedEmail);
        ensureAdminPrivileges(admin);
      }
      return;
    }

    User admin = new User();
    admin.setUsername("admin");
    admin.setEmail(normalizedEmail);
    admin.setPassword(passwordEncoder.encode(normalizedAdminPassword()));
    ensureAdminPrivileges(admin);
  }

  private void ensureAdminPrivileges(User admin) {
    admin.setRole(Role.ADMIN);
    admin.setVerified(true);
    admin.setApproved(true);
    if (admin.getAvatar() == null || admin.getAvatar().isBlank()) {
      admin.setAvatar("/whu-emblem.webp");
    }
    if (admin.getIntroduction() == null || admin.getIntroduction().isBlank()) {
      admin.setIntroduction("珞珈论坛初始化管理员");
    }
    if (admin.getRegisterReason() == null || admin.getRegisterReason().isBlank()) {
      admin.setRegisterReason("珞珈论坛初始化管理员");
    }
    userRepository.save(admin);
  }

  private String normalizedAdminPassword() {
    return adminPassword == null || adminPassword.isBlank() ? "123456" : adminPassword;
  }
}
