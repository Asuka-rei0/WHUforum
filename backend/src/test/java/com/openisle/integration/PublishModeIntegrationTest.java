package com.openisle.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.config.CachingConfig;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.EmailSender;
import com.openisle.service.PushNotificationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;

/** Integration tests for review publish mode. */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "app.post.publish-mode=REVIEW",
    "app.register.mode=DIRECT",
    "app.whu.mode=false",
    "management.health.redis.enabled=false",
  }
)
class PublishModeIntegrationTest {

  @Autowired
  private TestRestTemplate rest;

  @Autowired
  private UserRepository users;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @MockBean
  private CacheManager cacheManager;

  @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
  private RedisConnectionFactory redisConnectionFactory;

  @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
  private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

  @MockBean
  private EmailSender emailService;

  @BeforeEach
  void setUpCacheManager() {
    when(cacheManager.getCache(anyString()))
      .thenAnswer(invocation -> new ConcurrentMapCache(invocation.getArgument(0)));
  }

  private String registerAndLogin(String username, String email) {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    rest.postForEntity(
      "/api/auth/register",
      new HttpEntity<>(
        Map.of(
          "username",
          username,
          "email",
          email,
          "password",
          "pass123",
          "reason",
          "integration test reason more than twenty"
        ),
        h
      ),
      Map.class
    );
    User u = users.findByUsername(username).orElseThrow();
    if (!u.isVerified()) {
      u.setVerified(true);
      users.save(u);
    }
    redisTemplate.delete(CachingConfig.LIMIT_CACHE_NAME + ":posts:" + username);
    ResponseEntity<Map> resp = rest.postForEntity(
      "/api/auth/login",
      new HttpEntity<>(Map.of("email", email, "password", "pass123"), h),
      Map.class
    );
    return (String) resp.getBody().get("token");
  }

  private String registerAndLoginAsAdmin(String username, String email) {
    String token = registerAndLogin(username, email);
    User u = users.findByUsername(username).orElseThrow();
    u.setRole(Role.ADMIN);
    users.save(u);
    return token;
  }

  private ResponseEntity<Map> postJson(String url, Map<?, ?> body, String token) {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) h.setBearerAuth(token);
    return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
  }

  private <T> ResponseEntity<T> get(String url, Class<T> type, String token) {
    HttpHeaders h = new HttpHeaders();
    if (token != null) h.setBearerAuth(token);
    return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(h), type);
  }

  @Test
  void postRequiresApproval() {
    String userToken = registerAndLogin("eve123", "eve123@whu.edu.cn");
    String adminToken = registerAndLoginAsAdmin("admin1", "admin1@whu.edu.cn");

    ResponseEntity<Map> catResp = postJson(
      "/api/categories",
      Map.of("name", "review", "description", "d", "icon", "i"),
      adminToken
    );
    Long catId = ((Number) catResp.getBody().get("id")).longValue();

    ResponseEntity<Map> tagResp = postJson(
      "/api/tags",
      Map.of("name", "t1", "description", "d", "icon", "i"),
      adminToken
    );
    Long tagId = ((Number) tagResp.getBody().get("id")).longValue();

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of("title", "Need", "content", "Review", "categoryId", catId, "tagIds", List.of(tagId)),
      userToken
    );
    Long postId = ((Number) postResp.getBody().get("id")).longValue();

    List<?> list = get("/api/posts", List.class, userToken).getBody();
    assertTrue(list.isEmpty(), "Post should not be listed before approval");

    List<Map<String, Object>> pending = get(
      "/api/admin/posts/pending",
      List.class,
      adminToken
    ).getBody();
    assertEquals(1, pending.size());
    assertEquals(postId.intValue(), ((Number) pending.get(0).get("id")).intValue());

    redisTemplate.delete(CachingConfig.LIMIT_CACHE_NAME + ":posts:eve123");
    ResponseEntity<Map> publicTreeholeResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Public treehole",
        "content",
        "Review",
        "categoryId",
        catId,
        "tagIds",
        List.of(tagId),
        "type",
        "TREEHOLE",
        "treeholeExpectedVisibility",
        "PUBLIC"
      ),
      userToken
    );
    assertEquals("AI_REVIEWING", publicTreeholeResp.getBody().get("treeholeReviewStatus"));

    redisTemplate.delete(CachingConfig.LIMIT_CACHE_NAME + ":posts:eve123");
    ResponseEntity<Map> privateTreeholeResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Private treehole",
        "content",
        "Review",
        "categoryId",
        catId,
        "tagIds",
        List.of(tagId),
        "type",
        "TREEHOLE"
      ),
      userToken
    );
    assertEquals("PRIVATE", privateTreeholeResp.getBody().get("treeholeReviewStatus"));

    List<Map<String, Object>> pendingAfterTreeholes = get(
      "/api/admin/posts/pending",
      List.class,
      adminToken
    ).getBody();
    assertEquals(1, pendingAfterTreeholes.size());
    assertEquals(postId.intValue(), ((Number) pendingAfterTreeholes.get(0).get("id")).intValue());

    postJson("/api/admin/posts/" + postId + "/approve", Map.of(), adminToken);

    List<?> listAfter = get("/api/posts", List.class, userToken).getBody();
    assertEquals(1, listAfter.size(), "Post should appear after approval");
  }
}
