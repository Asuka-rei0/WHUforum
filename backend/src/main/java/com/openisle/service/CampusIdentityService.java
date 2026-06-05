package com.openisle.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CampusIdentityService {

  private static final SecureRandom RANDOM = new SecureRandom();

  @Value("${app.whu.identity-hash-salt:whuforum-dev-salt}")
  private String hashSalt;

  @Value("${app.whu.identity-encryption-key:}")
  private String encryptionKey;

  public String hashCampusId(String campusId) {
    if (campusId == null || campusId.isBlank()) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(
        (hashSalt + ":" + campusId.trim()).getBytes(StandardCharsets.UTF_8)
      );
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to hash campus id", e);
    }
  }

  public String encryptCampusId(String campusId) {
    if (
      campusId == null || campusId.isBlank() || encryptionKey == null || encryptionKey.isBlank()
    ) {
      return null;
    }
    try {
      byte[] keyBytes = Arrays.copyOf(
        MessageDigest.getInstance("SHA-256").digest(encryptionKey.getBytes(StandardCharsets.UTF_8)),
        32
      );
      byte[] iv = new byte[12];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(keyBytes, "AES"),
        new GCMParameterSpec(128, iv)
      );
      byte[] encrypted = cipher.doFinal(campusId.trim().getBytes(StandardCharsets.UTF_8));
      byte[] packed = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, packed, 0, iv.length);
      System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
      return Base64.getEncoder().encodeToString(packed);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to encrypt campus id", e);
    }
  }
}
