package com.openisle.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openisle.exception.EmailSendException;
import com.openisle.model.RegisterMode;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.*;
import com.openisle.util.VerifyType;
import io.jsonwebtoken.JwtException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AuthController.class, properties = "app.whu.mode=false")
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private JwtService jwtService;

  @MockBean
  private EmailSender emailService;

  @MockBean
  private CaptchaService captchaService;

  @MockBean
  private GoogleAuthService googleAuthService;

  @MockBean
  private RegisterModeService registerModeService;

  @MockBean
  private GithubAuthService githubAuthService;

  @MockBean
  private DiscordAuthService discordAuthService;

  @MockBean
  private TwitterAuthService twitterAuthService;

  @MockBean
  private TelegramAuthService telegramAuthService;

  @MockBean
  private NotificationService notificationService;

  @MockBean
  private InviteService inviteService;

  @MockBean
  private UserRepository userRepository;

  @Test
  void registerSendsEmail() throws Exception {
    User user = new User();
    user.setEmail("a@whu.edu.cn");
    user.setUsername("u");
    user.setVerificationCode("123456");
    Mockito.when(registerModeService.getRegisterMode()).thenReturn(RegisterMode.DIRECT);
    Mockito.when(
      userService.register(eq("u"), eq("a@whu.edu.cn"), eq("p"), any(), eq(RegisterMode.DIRECT))
    ).thenReturn(user);

    mockMvc
      .perform(
        post("/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"username\":\"u\",\"email\":\"a@whu.edu.cn\",\"password\":\"p\",\"reason\":\"test reason more than twenty\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").exists());

    Mockito.verify(userService).sendVerifyMail(user, VerifyType.REGISTER);
  }

  @Test
  void registerHidesRawEmailProviderError() throws Exception {
    User user = new User();
    user.setEmail("a@whu.edu.cn");
    user.setUsername("u");
    Mockito.when(registerModeService.getRegisterMode()).thenReturn(RegisterMode.DIRECT);
    Mockito.when(
      userService.register(eq("u"), eq("a@whu.edu.cn"), eq("p"), any(), eq(RegisterMode.DIRECT))
    ).thenReturn(user);
    Mockito.doThrow(new EmailSendException("Failed to send email: 401 Unauthorized: [no body]"))
      .when(userService)
      .sendVerifyMail(user, VerifyType.REGISTER);

    mockMvc
      .perform(
        post("/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"username\":\"u\",\"email\":\"a@whu.edu.cn\",\"password\":\"p\"}")
      )
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.reason_code").value("EMAIL_SEND_FAILED"))
      .andExpect(jsonPath("$.error").value(not(containsString("401"))));
  }

  @Test
  void verifyCodeEndpoint() throws Exception {
    User user = new User();
    user.setUsername("u");
    user.setApproved(false);
    Mockito.when(userService.findByUsername("u")).thenReturn(Optional.of(user));
    Mockito.when(userService.verifyCode(user, "123", VerifyType.REGISTER)).thenReturn(true);
    Mockito.when(jwtService.generateReasonToken("u")).thenReturn("reason_token");

    mockMvc
      .perform(
        post("/api/auth/verify")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"username\":\"u\",\"code\":\"123\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value("Verified"));
  }

  @Test
  void activateEndpointReturnsApprovedToken() throws Exception {
    User user = new User();
    user.setUsername("u");
    user.setApproved(true);
    Mockito.when(userService.activateRegisterToken("activation-token")).thenReturn(
      Optional.of(user)
    );
    Mockito.when(jwtService.generateToken("u")).thenReturn("jwt");

    mockMvc
      .perform(
        post("/api/auth/activate")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"token\":\"activation-token\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.reason_code").value("VERIFIED_AND_APPROVED"))
      .andExpect(jsonPath("$.token").value("jwt"));
  }

  @Test
  void activateEndpointRejectsExpiredToken() throws Exception {
    Mockito.when(userService.activateRegisterToken("expired")).thenReturn(Optional.empty());

    mockMvc
      .perform(
        post("/api/auth/activate")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"token\":\"expired\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("Invalid or expired activation token"));
  }

  @Test
  void loginReturnsToken() throws Exception {
    User user = new User();
    user.setUsername("u");
    user.setEmail("u@whu.edu.cn");
    user.setVerified(true);
    Mockito.when(userService.findByEmail("u@whu.edu.cn")).thenReturn(Optional.of(user));
    Mockito.when(userService.matchesPassword(user, "p")).thenReturn(true);
    Mockito.when(jwtService.generateToken("u")).thenReturn("token");

    mockMvc
      .perform(
        post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"email\":\"u@whu.edu.cn\",\"password\":\"p\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").value("token"));
  }

  @Test
  void loginFailsWhenEmailIsNotRegistered() throws Exception {
    Mockito.when(userService.findByEmail("u@whu.edu.cn")).thenReturn(Optional.empty());

    mockMvc
      .perform(
        post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"email\":\"u@whu.edu.cn\",\"password\":\"bad\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.field").value("email"))
      .andExpect(jsonPath("$.reason_code").value("EMAIL_NOT_REGISTERED"));
  }

  @Test
  void loginFailsWhenPasswordIsInvalid() throws Exception {
    User user = new User();
    user.setUsername("u");
    user.setEmail("u@whu.edu.cn");
    user.setVerified(true);
    Mockito.when(userService.findByEmail("u@whu.edu.cn")).thenReturn(Optional.of(user));
    Mockito.when(userService.matchesPassword(user, "bad")).thenReturn(false);

    mockMvc
      .perform(
        post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"email\":\"u@whu.edu.cn\",\"password\":\"bad\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.field").value("password"))
      .andExpect(jsonPath("$.reason_code").value("INVALID_PASSWORD"));
  }

  @Test
  void forgotPasswordSendsResetLinkEmail() throws Exception {
    User user = new User();
    user.setUsername("u");
    user.setEmail("student@whu.edu.cn");
    Mockito.when(userService.findByEmail("student@whu.edu.cn")).thenReturn(Optional.of(user));
    Mockito.when(jwtService.generateResetToken("u")).thenReturn("reset-jwt");

    mockMvc
      .perform(
        post("/api/auth/forgot/send")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"email\":\" Student@WHU.EDU.CN \"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value("Password reset email sent"));

    Mockito.verify(userService).sendPasswordResetMail(user, "reset-jwt");
    Mockito.verify(userService, Mockito.never()).sendVerifyMail(user, VerifyType.RESET_PASSWORD);
  }

  @Test
  void resetPasswordRejectsInvalidResetToken() throws Exception {
    Mockito.when(jwtService.validateAndGetSubjectForReset("bad-token")).thenThrow(
      new JwtException("expired")
    );

    mockMvc
      .perform(
        post("/api/auth/forgot/reset")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"token\":\"bad-token\",\"password\":\"new-password\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.reason_code").value("INVALID_RESET_TOKEN"));
  }
}
