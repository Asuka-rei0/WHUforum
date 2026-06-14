package com.openisle.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.config.CachingConfig;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.EmailSender;
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

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "app.post.publish-mode=DIRECT",
    "app.register.mode=DIRECT",
    "app.whu.mode=false",
    "management.health.redis.enabled=false",
  }
)
class TreeholePostIntegrationTest {

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
    clearPostLimit(username);
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

  private void clearPostLimit(String username) {
    redisTemplate.delete(CachingConfig.LIMIT_CACHE_NAME + ":posts:" + username);
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

  private PostTarget createPostTarget(String adminToken, String suffix) {
    ResponseEntity<Map> catResp = postJson(
      "/api/categories",
      Map.of("name", "treeholecat" + suffix, "description", "d", "icon", "i"),
      adminToken
    );
    Long catId = ((Number) catResp.getBody().get("id")).longValue();

    ResponseEntity<Map> tagResp = postJson(
      "/api/tags",
      Map.of("name", "treeholetag" + suffix, "description", "d", "icon", "i"),
      adminToken
    );
    Long tagId = ((Number) tagResp.getBody().get("id")).longValue();
    return new PostTarget(catId, tagId);
  }

  @Test
  void publicTreeholeStartsAiReviewingAndIsAuthorOnly() {
    String adminToken = registerAndLoginAsAdmin("th_admin1", "th_admin1@whu.edu.cn");
    String authorToken = registerAndLogin("th_author1", "th_author1@whu.edu.cn");
    String otherToken = registerAndLogin("th_other1", "th_other1@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "public");

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Treehole public",
        "content",
        "Content",
        "categoryId",
        target.categoryId(),
        "tagIds",
        List.of(target.tagId()),
        "type",
        "TREEHOLE",
        "treeholeExpectedVisibility",
        "PUBLIC",
        "anonymous",
        false,
        "postVisibleScopeType",
        "ALL"
      ),
      authorToken
    );

    assertEquals(HttpStatus.OK, postResp.getStatusCode());
    Map body = postResp.getBody();
    Long postId = ((Number) body.get("id")).longValue();
    assertEquals("TREEHOLE", body.get("type"));
    assertEquals(true, body.get("anonymous"));
    assertNotNull(body.get("anonymousAlias"));
    assertEquals("PUBLIC", body.get("treeholeExpectedVisibility"));
    assertEquals("AI_REVIEWING", body.get("treeholeReviewStatus"));
    assertEquals("PENDING", body.get("status"));
    assertEquals("ONLY_ME", body.get("visibleScope"));

    ResponseEntity<Map> authorDetail = get("/api/posts/" + postId, Map.class, authorToken);
    assertEquals(HttpStatus.OK, authorDetail.getStatusCode());
    assertEquals("AI_REVIEWING", authorDetail.getBody().get("treeholeReviewStatus"));

    ResponseEntity<Map> otherDetail = get("/api/posts/" + postId, Map.class, otherToken);
    assertEquals(HttpStatus.NOT_FOUND, otherDetail.getStatusCode());
  }

  @Test
  void privateTreeholeDefaultsToOnlyMe() {
    String adminToken = registerAndLoginAsAdmin("th_admin2", "th_admin2@whu.edu.cn");
    String authorToken = registerAndLogin("th_author2", "th_author2@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "private");

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Treehole private",
        "content",
        "Content",
        "categoryId",
        target.categoryId(),
        "tagIds",
        List.of(target.tagId()),
        "type",
        "TREEHOLE"
      ),
      authorToken
    );

    assertEquals(HttpStatus.OK, postResp.getStatusCode());
    Map body = postResp.getBody();
    assertEquals("TREEHOLE", body.get("type"));
    assertEquals(true, body.get("anonymous"));
    assertNotNull(body.get("anonymousAlias"));
    assertEquals("ONLY_ME", body.get("treeholeExpectedVisibility"));
    assertEquals("PRIVATE", body.get("treeholeReviewStatus"));
    assertEquals("PENDING", body.get("status"));
    assertEquals("ONLY_ME", body.get("visibleScope"));
  }

  @Test
  void normalPostKeepsExistingDirectFlow() {
    String adminToken = registerAndLoginAsAdmin("th_admin3", "th_admin3@whu.edu.cn");
    String authorToken = registerAndLogin("th_author3", "th_author3@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "normal");

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Normal post",
        "content",
        "Content",
        "categoryId",
        target.categoryId(),
        "tagIds",
        List.of(target.tagId()),
        "anonymous",
        false
      ),
      authorToken
    );

    assertEquals(HttpStatus.OK, postResp.getStatusCode());
    Map body = postResp.getBody();
    assertEquals("NORMAL", body.get("type"));
    assertEquals(false, body.get("anonymous"));
    assertNull(body.get("treeholeExpectedVisibility"));
    assertNull(body.get("treeholeReviewStatus"));
    assertEquals("PUBLISHED", body.get("status"));
    assertEquals("ALL", body.get("visibleScope"));
  }

  private record PostTarget(Long categoryId, Long tagId) {}
}
