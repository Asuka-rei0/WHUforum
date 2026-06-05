package com.openisle.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openisle.config.CustomAccessDeniedHandler;
import com.openisle.config.SecurityConfig;
import com.openisle.exception.EmailSendException;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.AnonymousAuditService;
import com.openisle.service.EmailSender;
import com.openisle.service.JwtService;
import com.openisle.service.SensitiveWordService;
import com.openisle.service.UserVisitService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, CustomAccessDeniedHandler.class })
class AdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtService jwtService;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private UserVisitService userVisitService;

  @MockBean
  private AnonymousAuditService anonymousAuditService;

  @MockBean
  private SensitiveWordService sensitiveWordService;

  @MockBean
  private EmailSender emailSender;

  @Test
  void adminHelloReturnsMessage() throws Exception {
    Mockito.when(jwtService.validateAndGetSubject("adminToken")).thenReturn("admin");
    User admin = new User();
    admin.setUsername("admin");
    admin.setPassword("p");
    admin.setEmail("a@b.com");
    admin.setRole(Role.ADMIN);
    Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

    mockMvc
      .perform(get("/api/admin/hello").header("Authorization", "Bearer adminToken"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value("Hello, Admin User"));
  }

  @Test
  void adminHelloMissingToken() throws Exception {
    mockMvc
      .perform(get("/api/admin/hello"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error").value("Missing token"));
  }

  @Test
  void adminHelloInvalidToken() throws Exception {
    Mockito.when(jwtService.validateAndGetSubject("bad")).thenThrow(new RuntimeException());

    mockMvc
      .perform(get("/api/admin/hello").header("Authorization", "Bearer bad"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error").value("Invalid or expired token"));
  }

  @Test
  void adminHelloNotAdmin() throws Exception {
    Mockito.when(jwtService.validateAndGetSubject("userToken")).thenReturn("user");
    User user = new User();
    user.setUsername("user");
    user.setPassword("p");
    user.setEmail("u@example.com");
    user.setRole(Role.USER);
    Mockito.when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

    mockMvc
      .perform(get("/api/admin/hello").header("Authorization", "Bearer userToken"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void adminCanSendEmailDeliveryTest() throws Exception {
    mockAdminToken();

    mockMvc
      .perform(
        post("/api/admin/email/test")
          .header("Authorization", "Bearer adminToken")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"to\":\" Student@WHU.EDU.CN \"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.sent").value(true))
      .andExpect(jsonPath("$.to").value("student@whu.edu.cn"))
      .andExpect(jsonPath("$.testCode").exists());

    Mockito.verify(emailSender).sendEmail(
      Mockito.eq("student@whu.edu.cn"),
      Mockito.eq("珞珈论坛邮件投递测试"),
      Mockito.contains("测试验证码"),
      Mockito.argThat(
        data ->
          "test".equals(data.get("_purpose")) &&
          "student@whu.edu.cn".equals(data.get("email")) &&
          data.get("token") != null
      )
    );
  }

  @Test
  void adminEmailDeliveryTestRejectsNonWhuEmail() throws Exception {
    mockAdminToken();

    mockMvc
      .perform(
        post("/api/admin/email/test")
          .header("Authorization", "Bearer adminToken")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"to\":\"student@example.com\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.reason_code").value("WHU_EMAIL_REQUIRED"));

    Mockito.verifyNoInteractions(emailSender);
  }

  @Test
  void adminEmailDeliveryTestMapsSenderFailure() throws Exception {
    mockAdminToken();
    Mockito.doThrow(new EmailSendException("SMTP_PASSWORD is not configured"))
      .when(emailSender)
      .sendEmail(Mockito.eq("student@whu.edu.cn"), Mockito.any(), Mockito.any(), Mockito.anyMap());

    mockMvc
      .perform(
        post("/api/admin/email/test")
          .header("Authorization", "Bearer adminToken")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"to\":\"student@whu.edu.cn\"}")
      )
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.reason_code").value("EMAIL_SEND_FAILED"));
  }

  private void mockAdminToken() {
    Mockito.when(jwtService.validateAndGetSubject("adminToken")).thenReturn("admin");
    User admin = new User();
    admin.setUsername("admin");
    admin.setPassword("p");
    admin.setEmail("a@whu.edu.cn");
    admin.setRole(Role.ADMIN);
    Mockito.when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
  }
}
