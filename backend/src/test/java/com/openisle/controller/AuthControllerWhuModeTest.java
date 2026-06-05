package com.openisle.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openisle.model.RegisterMode;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.CaptchaService;
import com.openisle.service.DiscordAuthService;
import com.openisle.service.EmailSender;
import com.openisle.service.GithubAuthService;
import com.openisle.service.GoogleAuthService;
import com.openisle.service.InviteService;
import com.openisle.service.JwtService;
import com.openisle.service.NotificationService;
import com.openisle.service.RegisterModeService;
import com.openisle.service.TelegramAuthService;
import com.openisle.service.TwitterAuthService;
import com.openisle.service.UserService;
import com.openisle.util.VerifyType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AuthController.class, properties = "app.whu.mode=true")
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWhuModeTest {

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
  void registerRejectsNonWhuEmail() throws Exception {
    mockMvc
      .perform(
        post("/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"username\":\"u\",\"email\":\"a@example.com\",\"password\":\"p\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.field").value("email"))
      .andExpect(jsonPath("$.reason_code").value("WHU_EMAIL_REQUIRED"));

    verify(userService, never()).register(any(), any(), any(), any(), any());
  }

  @Test
  void registerAllowsWhuEmail() throws Exception {
    User user = new User();
    user.setEmail("student@whu.edu.cn");
    user.setUsername("u");
    user.setApproved(true);
    Mockito.when(registerModeService.getRegisterMode()).thenReturn(RegisterMode.DIRECT);
    Mockito.when(
      userService.register(
        eq("u"),
        eq("student@whu.edu.cn"),
        eq("p"),
        any(),
        eq(RegisterMode.DIRECT)
      )
    ).thenReturn(user);

    mockMvc
      .perform(
        post("/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"username\":\"u\",\"email\":\" Student@WHU.EDU.CN \",\"password\":\"p\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").exists());

    verify(userService).sendVerifyMail(user, VerifyType.REGISTER);
  }

  @Test
  void casCallbackReturnsEmailAuthOnly() throws Exception {
    mockMvc
      .perform(
        post("/api/auth/cas/callback")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"state\":\"state-1\",\"ticket\":\"ticket-1\",\"inviteToken\":\"invite-1\"}")
      )
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.reason_code").value("EMAIL_AUTH_ONLY"));

    verify(userService, never()).upsertCampusUserWithResult(any());
  }
}
