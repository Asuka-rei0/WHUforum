package com.openisle.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.openisle.config.CachingConfig;
import com.openisle.controller.ActivityController;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.AvatarGenerator;
import com.openisle.service.EmailSender;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = { "app.register.mode=DIRECT", "app.whu.mode=false" }
)
class ComplexFlowIntegrationTest {

  @Autowired
  private TestRestTemplate rest;

  @Autowired
  private UserRepository users;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @MockBean
  private EmailSender emailService;

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
  void nestedCommentsVisibleInPost() {
    String t1 = registerAndLogin("alice1", "alice1@whu.edu.cn");
    String t2 = registerAndLogin("bob123", "bob123@whu.edu.cn");

    String adminToken = registerAndLoginAsAdmin("admin1", "admin1@whu.edu.cn");
    ResponseEntity<Map> catResp = postJson(
      "/api/categories",
      Map.of("name", "general", "description", "d", "icon", "i"),
      adminToken
    );
    Long catId = ((Number) catResp.getBody().get("id")).longValue();

    ResponseEntity<Map> tagResp = postJson(
      "/api/tags",
      Map.of("name", "java", "description", "d", "icon", "i"),
      adminToken
    );
    Long tagId = ((Number) tagResp.getBody().get("id")).longValue();

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of("title", "Hello", "content", "World", "categoryId", catId, "tagIds", List.of(tagId)),
      t1
    );
    Long postId = ((Number) postResp.getBody().get("id")).longValue();

    ResponseEntity<Map> c1Resp = postJson(
      "/api/posts/" + postId + "/comments",
      Map.of("content", "first"),
      t2
    );
    Long c1 = ((Number) c1Resp.getBody().get("id")).longValue();

    ResponseEntity<Map> r1Resp = postJson(
      "/api/comments/" + c1 + "/replies",
      Map.of("content", "reply1"),
      t1
    );
    Long r1 = ((Number) r1Resp.getBody().get("id")).longValue();

    postJson("/api/comments/" + r1 + "/replies", Map.of("content", "reply2"), t2);

    Map post = get("/api/posts/" + postId, Map.class, t1).getBody();
    assertEquals("Hello", post.get("title"));
    List<?> comments = (List<?>) post.get("comments");
    assertEquals(1, comments.size());
    Map<?, ?> cMap = (Map<?, ?>) comments.get(0);
    assertEquals("first", cMap.get("content"));
    List<?> replies1 = (List<?>) cMap.get("replies");
    assertEquals(1, replies1.size());
    Map<?, ?> rMap = (Map<?, ?>) replies1.get(0);
    assertEquals("reply1", rMap.get("content"));
    List<?> replies2 = (List<?>) rMap.get("replies");
    assertEquals(1, replies2.size());
    assertEquals("reply2", ((Map<?, ?>) replies2.get(0)).get("content"));
  }

  @Test
  void reactionsReturnedForPostAndComment() {
    String t1 = registerAndLogin("carol1", "carol1@whu.edu.cn");
    String t2 = registerAndLogin("dave01", "dave01@whu.edu.cn");

    String adminToken = registerAndLoginAsAdmin("admin2", "admin2@whu.edu.cn");
    List<Map<String, Object>> categories = (List<Map<String, Object>>) rest.getForObject(
      "/api/categories",
      List.class
    );
    Long catId = null;
    if (categories != null) {
      for (Map<String, Object> cat : categories) {
        if ("general".equals(cat.get("name"))) {
          catId = ((Number) cat.get("id")).longValue();
          break;
        }
      }
    }

    if (catId == null) {
      ResponseEntity<Map> catResp = postJson(
        "/api/categories",
        Map.of("name", "general", "description", "d", "icon", "i"),
        adminToken
      );
      catId = ((Number) catResp.getBody().get("id")).longValue();
    }

    ResponseEntity<Map> tagResp = postJson(
      "/api/tags",
      Map.of("name", "spring", "description", "d", "icon", "i"),
      adminToken
    );
    Long tagId = ((Number) tagResp.getBody().get("id")).longValue();

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of("title", "React", "content", "Test", "categoryId", catId, "tagIds", List.of(tagId)),
      t1
    );
    Long postId = ((Number) postResp.getBody().get("id")).longValue();

    postJson("/api/posts/" + postId + "/reactions", Map.of("type", "LIKE"), t2);

    ResponseEntity<Map> cResp = postJson(
      "/api/posts/" + postId + "/comments",
      Map.of("content", "hi"),
      t1
    );
    Long commentId = ((Number) cResp.getBody().get("id")).longValue();

    postJson("/api/comments/" + commentId + "/reactions", Map.of("type", "DISLIKE"), t2);

    Map post = get("/api/posts/" + postId, Map.class, t1).getBody();
    List<?> reactions = (List<?>) post.get("reactions");
    assertEquals(1, reactions.size());
    assertEquals("LIKE", ((Map<?, ?>) reactions.get(0)).get("type"));

    List<?> comments = (List<?>) post.get("comments");
    Map<?, ?> comment = (Map<?, ?>) comments.get(0);
    List<?> creactions = (List<?>) comment.get("reactions");
    assertEquals(1, creactions.size());
    assertEquals("DISLIKE", ((Map<?, ?>) creactions.get(0)).get("type"));
  }
}
