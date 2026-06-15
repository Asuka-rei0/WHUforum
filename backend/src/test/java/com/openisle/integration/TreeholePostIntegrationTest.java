package com.openisle.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.config.CachingConfig;
import com.openisle.event.TreeholeReviewRequestedEvent;
import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.PostVisibleScopeType;
import com.openisle.model.Role;
import com.openisle.model.TreeholeInterventionAction;
import com.openisle.model.TreeholeRiskLevel;
import com.openisle.model.TreeholeReviewStatus;
import com.openisle.model.User;
import com.openisle.repository.PostRepository;
import com.openisle.repository.TreeholeInterventionRecordRepository;
import com.openisle.repository.UserRepository;
import com.openisle.service.EmailSender;
import com.openisle.service.TreeholeReviewService;
import java.time.LocalDateTime;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
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
    "app.moderation.blocked-words=hardblock",
    "app.moderation.crisis-words=crisisword",
    "app.treehole.ai-review.enabled=false",
  }
)
class TreeholePostIntegrationTest {

  @Autowired
  private TestRestTemplate rest;

  @Autowired
  private UserRepository users;

  @Autowired
  private PostRepository posts;

  @Autowired
  private TreeholeInterventionRecordRepository interventionRecords;

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

  @SpyBean
  private TreeholeReviewService treeholeReviewService;

  @BeforeEach
  void setUpCacheManager() {
    when(cacheManager.getCache(anyString()))
      .thenAnswer(invocation -> new ConcurrentMapCache(invocation.getArgument(0)));
    clearInvocations(treeholeReviewService);
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

  private ResponseEntity<List> getList(String url, String token) {
    HttpHeaders h = new HttpHeaders();
    if (token != null) h.setBearerAuth(token);
    return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(h), List.class);
  }

  private ResponseEntity<String> getString(String url, String token) {
    HttpHeaders h = new HttpHeaders();
    if (token != null) h.setBearerAuth(token);
    return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(h), String.class);
  }

  private ResponseEntity<Map> postEmpty(String url, String token) {
    HttpHeaders h = new HttpHeaders();
    if (token != null) h.setBearerAuth(token);
    return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(h), Map.class);
  }

  private PostTarget createPostTarget(String adminToken, String suffix) {
    ResponseEntity<Map> catResp = postJson(
      "/api/categories",
      Map.of("name", "treeholecat" + suffix, "description", "d", "icon", "i"),
      adminToken
    );
    assertEquals(HttpStatus.OK, catResp.getStatusCode(), String.valueOf(catResp.getBody()));
    Long catId = ((Number) catResp.getBody().get("id")).longValue();

    ResponseEntity<Map> tagResp = postJson(
      "/api/tags",
      Map.of("name", "treeholetag" + suffix, "description", "d", "icon", "i"),
      adminToken
    );
    assertEquals(HttpStatus.OK, tagResp.getStatusCode(), String.valueOf(tagResp.getBody()));
    Long tagId = ((Number) tagResp.getBody().get("id")).longValue();
    return new PostTarget(catId, tagId);
  }

  private Long createTreehole(String authorToken, PostTarget target, String suffix, String visibility) {
    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Treehole " + suffix,
        "content",
        "Content " + suffix,
        "categoryId",
        target.categoryId(),
        "tagIds",
        List.of(target.tagId()),
        "type",
        "TREEHOLE",
        "treeholeExpectedVisibility",
        visibility
      ),
      authorToken
    );
    assertEquals(HttpStatus.OK, postResp.getStatusCode());
    return ((Number) postResp.getBody().get("id")).longValue();
  }

  private void markTreeholePublic(Long postId) {
    Post post = posts.findById(postId).orElseThrow();
    post.setTreeholeReviewStatus(TreeholeReviewStatus.PUBLIC);
    post.setStatus(PostStatus.PUBLISHED);
    post.setVisibleScope(PostVisibleScopeType.ALL);
    posts.save(post);
  }

  private void markTreeholeStatus(Long postId, TreeholeReviewStatus reviewStatus) {
    Post post = posts.findById(postId).orElseThrow();
    post.setTreeholeReviewStatus(reviewStatus);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
    posts.save(post);
  }

  private void markTreeholeRisk(
    Long postId,
    TreeholeRiskLevel riskLevel,
    TreeholeReviewStatus reviewStatus
  ) {
    Post post = posts.findById(postId).orElseThrow();
    post.setTreeholeRiskLevel(riskLevel);
    post.setTreeholeRiskReason("risk reason " + riskLevel);
    post.setTreeholeRecommendedAction("recommended action " + riskLevel);
    post.setTreeholeReviewedAt(LocalDateTime.now());
    post.setTreeholeReviewStatus(reviewStatus);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
    posts.save(post);
  }

  private boolean containsPostId(List body, Long postId) {
    return body.stream().anyMatch(item -> {
      Map post = (Map) item;
      return ((Number) post.get("id")).longValue() == postId.longValue();
    });
  }

  private boolean containsCasePostId(List body, Long postId) {
    return body.stream().anyMatch(item -> {
      Map treeholeCase = (Map) item;
      return ((Number) treeholeCase.get("postId")).longValue() == postId.longValue();
    });
  }

  private ResponseEntity<Map> postAction(
    Long postId,
    String action,
    String note,
    String adminToken
  ) {
    return postJson(
      "/api/admin/treeholes/" + postId + "/actions",
      Map.of("action", action, "note", note),
      adminToken
    );
  }

  private boolean recordsContainAction(Map detail, String action) {
    List records = (List) detail.get("records");
    return records
      .stream()
      .anyMatch(record -> action.equals(((Map) record).get("action")));
  }

  private boolean listContainsText(List body, String text) {
    return body.stream().anyMatch(item -> String.valueOf(item).contains(text));
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
    verify(treeholeReviewService, timeout(1000)).handleTreeholeReviewRequested(
      argThat(event ->
        event.postId().equals(postId) &&
        event.expectedVisibility().name().equals("PUBLIC") &&
        event.initialReviewStatus().name().equals("AI_REVIEWING") &&
        !event.moderationFlagged() &&
        !event.crisisFlagged()
      )
    );
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
    Long postId = ((Number) body.get("id")).longValue();
    verify(treeholeReviewService, timeout(1000)).handleTreeholeReviewRequested(
      argThat(event ->
        event.postId().equals(postId) &&
        event.expectedVisibility().name().equals("ONLY_ME") &&
        event.initialReviewStatus().name().equals("PRIVATE") &&
        !event.moderationFlagged() &&
        !event.crisisFlagged()
      )
    );
  }

  @Test
  void treeholeHardBlockedSensitiveWordFailsBeforeSave() {
    String adminToken = registerAndLoginAsAdmin("th_admin4", "th_admin4@whu.edu.cn");
    String authorToken = registerAndLogin("th_author4", "th_author4@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "hardblock");

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Treehole hardblock",
        "content",
        "Content",
        "categoryId",
        target.categoryId(),
        "tagIds",
        List.of(target.tagId()),
        "type",
        "TREEHOLE",
        "treeholeExpectedVisibility",
        "PUBLIC"
      ),
      authorToken
    );

    assertEquals(HttpStatus.BAD_REQUEST, postResp.getStatusCode());
    assertEquals("Post contains sensitive content", postResp.getBody().get("error"));
    verify(treeholeReviewService, never()).handleTreeholeReviewRequested(
      any(TreeholeReviewRequestedEvent.class)
    );
  }

  @Test
  void treeholeCrisisWordSucceedsAndRequestsReview() {
    String adminToken = registerAndLoginAsAdmin("th_admin5", "th_admin5@whu.edu.cn");
    String authorToken = registerAndLogin("th_author5", "th_author5@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "crisis");

    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Treehole crisisword",
        "content",
        "Content",
        "categoryId",
        target.categoryId(),
        "tagIds",
        List.of(target.tagId()),
        "type",
        "TREEHOLE",
        "treeholeExpectedVisibility",
        "PUBLIC"
      ),
      authorToken
    );

    assertEquals(HttpStatus.OK, postResp.getStatusCode());
    Map body = postResp.getBody();
    Long postId = ((Number) body.get("id")).longValue();
    assertEquals("AI_REVIEWING", body.get("treeholeReviewStatus"));
    assertEquals("PENDING", body.get("status"));
    assertEquals("ONLY_ME", body.get("visibleScope"));
    verify(treeholeReviewService, timeout(1000)).handleTreeholeReviewRequested(
      argThat(event ->
        event.postId().equals(postId) &&
        event.moderationFlagged() &&
        event.crisisFlagged() &&
        "crisisword".equals(event.matchedWord())
      )
    );
  }

  @Test
  void treeholeSquareAnonymousOnlySeesPublishedPublicTreeholes() {
    String adminToken = registerAndLoginAsAdmin("th_admin6", "th_admin6@whu.edu.cn");
    String authorToken = registerAndLogin("th_author6", "th_author6@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "squareanon");

    Long publicId = createTreehole(authorToken, target, "published", "PUBLIC");
    markTreeholePublic(publicId);
    clearPostLimit("th_author6");
    Long reviewingId = createTreehole(authorToken, target, "reviewing", "PUBLIC");

    ResponseEntity<List> squareResp = getList("/api/treeholes/square?page=0&pageSize=20", null);

    assertEquals(HttpStatus.OK, squareResp.getStatusCode());
    assertTrue(containsPostId(squareResp.getBody(), publicId));
    assertFalse(containsPostId(squareResp.getBody(), reviewingId));
  }

  @Test
  void treeholeSquareAuthorSeesOwnReviewingPublicTreehole() {
    String adminToken = registerAndLoginAsAdmin("th_admin7", "th_admin7@whu.edu.cn");
    String authorToken = registerAndLogin("th_author7", "th_author7@whu.edu.cn");
    String otherToken = registerAndLogin("th_other7", "th_other7@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "squareauthor");

    Long reviewingId = createTreehole(authorToken, target, "own-reviewing", "PUBLIC");

    ResponseEntity<List> authorSquare = getList(
      "/api/treeholes/square?page=0&pageSize=20",
      authorToken
    );
    ResponseEntity<List> otherSquare = getList(
      "/api/treeholes/square?page=0&pageSize=20",
      otherToken
    );

    assertEquals(HttpStatus.OK, authorSquare.getStatusCode());
    assertTrue(containsPostId(authorSquare.getBody(), reviewingId));
    assertEquals(HttpStatus.OK, otherSquare.getStatusCode());
    assertFalse(containsPostId(otherSquare.getBody(), reviewingId));
  }

  @Test
  void publicTreeholeDetailsCommentsAndReactionsStayAnonymous() {
    String adminToken = registerAndLoginAsAdmin("th_admin12", "th_admin12@whu.edu.cn");
    String authorToken = registerAndLogin("th_author12", "th_author12@whu.edu.cn");
    String commenterToken = registerAndLogin("th_commenter12", "th_commenter12@whu.edu.cn");
    String viewerToken = registerAndLogin("th_viewer12", "th_viewer12@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "identitysafe");

    Long postId = createTreehole(authorToken, target, "identity-safe", "PUBLIC");
    markTreeholePublic(postId);

    ResponseEntity<Map> commentResp = postJson(
      "/api/posts/" + postId + "/comments",
      Map.of("content", "treehole public anonymous comment"),
      commenterToken
    );
    assertEquals(HttpStatus.OK, commentResp.getStatusCode());
    Long commentId = ((Number) commentResp.getBody().get("id")).longValue();
    Map commentAuthor = (Map) commentResp.getBody().get("author");
    assertEquals(true, commentAuthor.get("anonymous"));
    assertNotEquals("th_commenter12", commentAuthor.get("username"));

    ResponseEntity<Map> reactionResp = postJson(
      "/api/posts/" + postId + "/reactions",
      Map.of("type", "LIKE"),
      commenterToken
    );
    assertEquals(HttpStatus.OK, reactionResp.getStatusCode());

    ResponseEntity<Map> detailResp = get("/api/posts/" + postId, Map.class, viewerToken);
    assertEquals(HttpStatus.OK, detailResp.getStatusCode());
    Map body = detailResp.getBody();
    Map author = (Map) body.get("author");
    assertEquals(true, author.get("anonymous"));
    assertNotEquals("th_author12", author.get("username"));
    assertFalse(listContainsText((List) body.get("participants"), "th_author12"));
    assertFalse(listContainsText((List) body.get("participants"), "th_commenter12"));
    assertFalse(listContainsText((List) body.get("reactions"), "th_commenter12"));
    assertTrue(((List) body.get("comments")).stream().allMatch(item -> {
      Map comment = (Map) item;
      Map itemAuthor = (Map) comment.get("author");
      return Boolean.TRUE.equals(itemAuthor.get("anonymous")) &&
        !"th_commenter12".equals(itemAuthor.get("username"));
    }));

    ResponseEntity<List> timelineResp = getList(
      "/api/posts/" + postId + "/comments?page=0&pageSize=20",
      viewerToken
    );
    assertEquals(HttpStatus.OK, timelineResp.getStatusCode());
    assertFalse(listContainsText(timelineResp.getBody(), "th_commenter12"));
    assertTrue(listContainsText(timelineResp.getBody(), String.valueOf(commentId)));
  }

  @Test
  void privateTreeholeCommentAndReactionEndpointsAreAuthorOnly() {
    String adminToken = registerAndLoginAsAdmin("th_admin13", "th_admin13@whu.edu.cn");
    String authorToken = registerAndLogin("th_author13", "th_author13@whu.edu.cn");
    String otherToken = registerAndLogin("th_other13", "th_other13@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "privateaccess");

    Long postId = createTreehole(authorToken, target, "private-access", "ONLY_ME");
    ResponseEntity<Map> authorCommentCreate = postJson(
      "/api/posts/" + postId + "/comments",
      Map.of("content", "private author comment"),
      authorToken
    );
    assertEquals(HttpStatus.OK, authorCommentCreate.getStatusCode());
    Long commentId = ((Number) authorCommentCreate.getBody().get("id")).longValue();

    ResponseEntity<List> authorComments = getList(
      "/api/posts/" + postId + "/comments?page=0&pageSize=20",
      authorToken
    );
    ResponseEntity<Map> otherComments = get(
      "/api/posts/" + postId + "/comments?page=0&pageSize=20",
      Map.class,
      otherToken
    );
    ResponseEntity<Map> otherCommentCreate = postJson(
      "/api/posts/" + postId + "/comments",
      Map.of("content", "should not pass"),
      otherToken
    );
    ResponseEntity<Map> otherReaction = postJson(
      "/api/posts/" + postId + "/reactions",
      Map.of("type", "LIKE"),
      otherToken
    );
    ResponseEntity<Map> otherPostSubscription = postEmpty(
      "/api/subscriptions/posts/" + postId,
      otherToken
    );
    ResponseEntity<Map> otherCommentSubscription = postEmpty(
      "/api/subscriptions/comments/" + commentId,
      otherToken
    );

    assertEquals(HttpStatus.OK, authorComments.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, otherComments.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, otherCommentCreate.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, otherReaction.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, otherPostSubscription.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, otherCommentSubscription.getStatusCode());
  }

  @Test
  void anonymousTreeholeDoesNotLeakThroughFollowerMentionOrReactionNotifications() {
    String adminToken = registerAndLoginAsAdmin("th_admin15", "th_admin15@whu.edu.cn");
    String authorToken = registerAndLogin("th_author15", "th_author15@whu.edu.cn");
    String followerToken = registerAndLogin("th_follower15", "th_follower15@whu.edu.cn");
    String mentionedToken = registerAndLogin("th_mentioned15", "th_mentioned15@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "notification");

    assertEquals(
      HttpStatus.OK,
      postEmpty("/api/subscriptions/users/th_author15", followerToken).getStatusCode()
    );
    clearPostLimit("th_author15");
    ResponseEntity<Map> postResp = postJson(
      "/api/posts",
      Map.of(
        "title",
        "Treehole notification leak secret",
        "content",
        "Content with @[th_mentioned15] notificationLeakSecret",
        "categoryId",
        target.categoryId(),
        "tagIds",
        List.of(target.tagId()),
        "type",
        "TREEHOLE",
        "treeholeExpectedVisibility",
        "PUBLIC"
      ),
      authorToken
    );
    assertEquals(HttpStatus.OK, postResp.getStatusCode());

    ResponseEntity<String> followerNotifications = getString("/api/notifications", followerToken);
    ResponseEntity<String> mentionedNotifications = getString("/api/notifications", mentionedToken);
    assertEquals(HttpStatus.OK, followerNotifications.getStatusCode());
    assertFalse(followerNotifications.getBody().contains("notificationLeakSecret"));
    assertFalse(followerNotifications.getBody().contains("th_author15"));
    assertEquals(HttpStatus.OK, mentionedNotifications.getStatusCode());
    assertFalse(mentionedNotifications.getBody().contains("notificationLeakSecret"));
    assertFalse(mentionedNotifications.getBody().contains("th_author15"));

    String reactionAuthorToken = registerAndLogin("th_author15b", "th_author15b@whu.edu.cn");
    String reactorToken = registerAndLogin("th_reactor15b", "th_reactor15b@whu.edu.cn");
    clearPostLimit("th_author15b");
    Long publicPostId = createTreehole(reactionAuthorToken, target, "reaction-notify", "PUBLIC");
    markTreeholePublic(publicPostId);
    ResponseEntity<Map> reactionResp = postJson(
      "/api/posts/" + publicPostId + "/reactions",
      Map.of("type", "LIKE"),
      reactorToken
    );
    assertEquals(HttpStatus.OK, reactionResp.getStatusCode());
    ResponseEntity<String> authorNotifications = getString(
      "/api/notifications",
      reactionAuthorToken
    );
    assertEquals(HttpStatus.OK, authorNotifications.getStatusCode());
    assertFalse(authorNotifications.getBody().contains("th_reactor15b"));
  }

  @Test
  void publicSearchAndUserAggregationsExcludeAnonymousTreeholeComments() {
    String adminToken = registerAndLoginAsAdmin("th_admin14", "th_admin14@whu.edu.cn");
    String authorToken = registerAndLogin("th_author14", "th_author14@whu.edu.cn");
    String commenterToken = registerAndLogin("th_commenter14", "th_commenter14@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "aggregation");

    Long postId = createTreehole(authorToken, target, "aggregation", "PUBLIC");
    markTreeholePublic(postId);
    ResponseEntity<Map> commentResp = postJson(
      "/api/posts/" + postId + "/comments",
      Map.of("content", "uniqueTreeholeAggregationSecret"),
      commenterToken
    );
    assertEquals(HttpStatus.OK, commentResp.getStatusCode());

    ResponseEntity<String> globalSearch = getString(
      "/api/search/global?keyword=uniqueTreeholeAggregationSecret",
      commenterToken
    );
    ResponseEntity<String> userReplies = getString(
      "/api/users/th_commenter14/replies",
      commenterToken
    );

    assertEquals(HttpStatus.OK, globalSearch.getStatusCode(), globalSearch.getBody());
    assertFalse(globalSearch.getBody().contains("uniqueTreeholeAggregationSecret"));
    assertEquals(HttpStatus.OK, userReplies.getStatusCode(), userReplies.getBody());
    assertFalse(userReplies.getBody().contains("uniqueTreeholeAggregationSecret"));
  }

  @Test
  void myTreeholesListsAllOwnTreeholeReviewStates() {
    String adminToken = registerAndLoginAsAdmin("th_admin8", "th_admin8@whu.edu.cn");
    String authorToken = registerAndLogin("th_author8", "th_author8@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "mine");

    Long reviewingId = createTreehole(authorToken, target, "mine-reviewing", "PUBLIC");
    clearPostLimit("th_author8");
    Long privateId = createTreehole(authorToken, target, "mine-private", "ONLY_ME");
    clearPostLimit("th_author8");
    Long publicId = createTreehole(authorToken, target, "mine-public", "PUBLIC");
    markTreeholePublic(publicId);
    clearPostLimit("th_author8");
    Long restrictedId = createTreehole(authorToken, target, "mine-restricted", "PUBLIC");
    markTreeholeStatus(restrictedId, TreeholeReviewStatus.PUBLIC_RESTRICTED);
    clearPostLimit("th_author8");
    Long reportedId = createTreehole(authorToken, target, "mine-reported", "PUBLIC");
    markTreeholeStatus(reportedId, TreeholeReviewStatus.REPORTED);

    ResponseEntity<List> mineResp = getList("/api/treeholes/me?page=0&pageSize=20", authorToken);

    assertEquals(HttpStatus.OK, mineResp.getStatusCode());
    assertTrue(containsPostId(mineResp.getBody(), reviewingId));
    assertTrue(containsPostId(mineResp.getBody(), privateId));
    assertTrue(containsPostId(mineResp.getBody(), publicId));
    assertTrue(containsPostId(mineResp.getBody(), restrictedId));
    assertTrue(containsPostId(mineResp.getBody(), reportedId));
  }

  @Test
  void adminRiskTreeholeListSupportsRiskAndStatusFilters() {
    String adminToken = registerAndLoginAsAdmin("th_admin9", "th_admin9@whu.edu.cn");
    String authorToken = registerAndLogin("th_author9", "th_author9@whu.edu.cn");
    String userToken = registerAndLogin("th_user9", "th_user9@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "adminrisk");

    Long l2Id = createTreehole(authorToken, target, "risk-l2", "PUBLIC");
    markTreeholeRisk(l2Id, TreeholeRiskLevel.L2, TreeholeReviewStatus.ADMIN_REVIEWING);
    clearPostLimit("th_author9");
    Long l3Id = createTreehole(authorToken, target, "risk-l3", "PUBLIC");
    markTreeholeRisk(l3Id, TreeholeRiskLevel.L3, TreeholeReviewStatus.PUBLIC_RESTRICTED);
    clearPostLimit("th_author9");
    Long l4Id = createTreehole(authorToken, target, "risk-l4", "PUBLIC");
    markTreeholeRisk(l4Id, TreeholeRiskLevel.L4, TreeholeReviewStatus.REPORTED);

    ResponseEntity<List> allResp = getList(
      "/api/admin/treeholes/risk?page=0&pageSize=20",
      adminToken
    );
    ResponseEntity<List> l3Resp = getList(
      "/api/admin/treeholes/risk?page=0&pageSize=20&riskLevel=L3",
      adminToken
    );
    ResponseEntity<List> openResp = getList(
      "/api/admin/treeholes/risk?page=0&pageSize=20&status=OPEN",
      adminToken
    );
    ResponseEntity<Map> forbiddenResp = get(
      "/api/admin/treeholes/risk?page=0&pageSize=20",
      Map.class,
      userToken
    );

    assertEquals(HttpStatus.OK, allResp.getStatusCode());
    assertTrue(containsCasePostId(allResp.getBody(), l2Id));
    assertTrue(containsCasePostId(allResp.getBody(), l3Id));
    assertTrue(containsCasePostId(allResp.getBody(), l4Id));
    assertEquals(HttpStatus.OK, l3Resp.getStatusCode());
    assertFalse(containsCasePostId(l3Resp.getBody(), l2Id));
    assertTrue(containsCasePostId(l3Resp.getBody(), l3Id));
    assertFalse(containsCasePostId(l3Resp.getBody(), l4Id));
    assertEquals(HttpStatus.OK, openResp.getStatusCode());
    assertTrue(containsCasePostId(openResp.getBody(), l2Id));
    assertNotEquals(HttpStatus.OK, forbiddenResp.getStatusCode());
  }

  @Test
  void adminTreeholeDetailRevealAuthorAndLegacyAnonymousAuditAreRecorded() {
    String adminToken = registerAndLoginAsAdmin("th_admin10", "th_admin10@whu.edu.cn");
    String authorToken = registerAndLogin("th_author10", "th_author10@whu.edu.cn");
    String userToken = registerAndLogin("th_user10", "th_user10@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "admindetail");

    Long postId = createTreehole(authorToken, target, "risk-detail", "PUBLIC");
    markTreeholeRisk(postId, TreeholeRiskLevel.L3, TreeholeReviewStatus.PUBLIC_RESTRICTED);
    getList("/api/admin/treeholes/risk?page=0&pageSize=20", adminToken);

    ResponseEntity<Map> detailResp = get("/api/admin/treeholes/" + postId, Map.class, adminToken);
    ResponseEntity<Map> forbiddenReveal = postJson(
      "/api/admin/treeholes/" + postId + "/reveal-author",
      Map.of("reason", "not admin"),
      userToken
    );
    ResponseEntity<Map> revealResp = postJson(
      "/api/admin/treeholes/" + postId + "/reveal-author",
      Map.of("reason", "risk intervention"),
      adminToken
    );
    getList("/api/admin/anonymous/posts/" + postId, adminToken);

    assertEquals(HttpStatus.OK, detailResp.getStatusCode());
    Map detail = detailResp.getBody();
    Map post = (Map) detail.get("post");
    Map caseInfo = (Map) detail.get("caseInfo");
    assertEquals(postId.longValue(), ((Number) post.get("id")).longValue());
    assertEquals("L3", post.get("treeholeRiskLevel"));
    assertEquals("PUBLIC_RESTRICTED", post.get("treeholeReviewStatus"));
    assertEquals("OPEN", caseInfo.get("status"));
    assertNotEquals(HttpStatus.OK, forbiddenReveal.getStatusCode());
    assertEquals(HttpStatus.OK, revealResp.getStatusCode());
    assertEquals("th_author10", revealResp.getBody().get("username"));
    assertEquals(
      2,
      interventionRecords.countByPost_IdAndAction(
        postId,
        TreeholeInterventionAction.VIEW_REAL_IDENTITY
      )
    );
  }

  @Test
  void adminTreeholeActionsUpdatePostCaseAndAppendRecords() {
    String adminToken = registerAndLoginAsAdmin("th_admin11", "th_admin11@whu.edu.cn");
    String authorToken = registerAndLogin("th_author11", "th_author11@whu.edu.cn");
    PostTarget target = createPostTarget(adminToken, "adminactions");

    Long allowId = createTreehole(authorToken, target, "allow-public", "PUBLIC");
    markTreeholeRisk(allowId, TreeholeRiskLevel.L2, TreeholeReviewStatus.ADMIN_REVIEWING);
    getList("/api/admin/treeholes/risk?page=0&pageSize=20", adminToken);

    ResponseEntity<Map> allowResp = postAction(
      allowId,
      "ALLOW_PUBLIC",
      "allow public",
      adminToken
    );
    Post allowPost = posts.findById(allowId).orElseThrow();
    ResponseEntity<Map> closedActionResp = postAction(
      allowId,
      "RESTRICT_PUBLIC",
      "should fail",
      adminToken
    );

    assertEquals(HttpStatus.OK, allowResp.getStatusCode());
    assertEquals("CLOSED", ((Map) allowResp.getBody().get("caseInfo")).get("status"));
    assertEquals(TreeholeReviewStatus.PUBLIC, allowPost.getTreeholeReviewStatus());
    assertEquals(PostStatus.PUBLISHED, allowPost.getStatus());
    assertEquals(PostVisibleScopeType.ALL, allowPost.getVisibleScope());
    assertTrue(recordsContainAction(allowResp.getBody(), "ALLOW_PUBLIC"));
    assertEquals(HttpStatus.BAD_REQUEST, closedActionResp.getStatusCode());

    clearPostLimit("th_author11");
    Long privateId = createTreehole(authorToken, target, "false-positive-private", "ONLY_ME");
    markTreeholeRisk(privateId, TreeholeRiskLevel.L2, TreeholeReviewStatus.ADMIN_REVIEWING);
    getList("/api/admin/treeholes/risk?page=0&pageSize=20", adminToken);
    ResponseEntity<Map> falsePositiveResp = postAction(
      privateId,
      "MARK_FALSE_POSITIVE",
      "false positive",
      adminToken
    );
    Post privatePost = posts.findById(privateId).orElseThrow();
    assertEquals(HttpStatus.OK, falsePositiveResp.getStatusCode());
    assertEquals("CLOSED", ((Map) falsePositiveResp.getBody().get("caseInfo")).get("status"));
    assertEquals(TreeholeReviewStatus.PRIVATE, privatePost.getTreeholeReviewStatus());
    assertEquals(PostStatus.PENDING, privatePost.getStatus());
    assertEquals(PostVisibleScopeType.ONLY_ME, privatePost.getVisibleScope());

    clearPostLimit("th_author11");
    Long restrictedId = createTreehole(authorToken, target, "restrict-flow", "PUBLIC");
    markTreeholeRisk(restrictedId, TreeholeRiskLevel.L4, TreeholeReviewStatus.REPORTED);
    getList("/api/admin/treeholes/risk?page=0&pageSize=20", adminToken);
    ResponseEntity<Map> restrictResp = postAction(
      restrictedId,
      "RESTRICT_PUBLIC",
      "restrict",
      adminToken
    );
    ResponseEntity<Map> contactedResp = postAction(
      restrictedId,
      "MARK_CONTACTED",
      "contacted",
      adminToken
    );
    ResponseEntity<Map> transferredResp = postAction(
      restrictedId,
      "MARK_TRANSFERRED",
      "transferred",
      adminToken
    );
    ResponseEntity<Map> noteResp = postAction(restrictedId, "UPDATE_NOTE", "latest note", adminToken);
    ResponseEntity<Map> closeResp = postAction(restrictedId, "CLOSE", "done", adminToken);
    Post restrictedPost = posts.findById(restrictedId).orElseThrow();

    assertEquals(HttpStatus.OK, restrictResp.getStatusCode());
    assertEquals("IN_PROGRESS", ((Map) restrictResp.getBody().get("caseInfo")).get("status"));
    assertEquals(TreeholeReviewStatus.PUBLIC_RESTRICTED, restrictedPost.getTreeholeReviewStatus());
    assertEquals(HttpStatus.OK, contactedResp.getStatusCode());
    assertEquals("CONTACTED", ((Map) contactedResp.getBody().get("caseInfo")).get("status"));
    assertEquals(HttpStatus.OK, transferredResp.getStatusCode());
    assertEquals("TRANSFERRED", ((Map) transferredResp.getBody().get("caseInfo")).get("status"));
    assertEquals(HttpStatus.OK, noteResp.getStatusCode());
    assertEquals("latest note", ((Map) noteResp.getBody().get("caseInfo")).get("note"));
    assertEquals(HttpStatus.OK, closeResp.getStatusCode());
    assertEquals("CLOSED", ((Map) closeResp.getBody().get("caseInfo")).get("status"));
    assertTrue(recordsContainAction(closeResp.getBody(), "RESTRICT_PUBLIC"));
    assertTrue(recordsContainAction(closeResp.getBody(), "MARK_CONTACTED"));
    assertTrue(recordsContainAction(closeResp.getBody(), "MARK_TRANSFERRED"));
    assertTrue(recordsContainAction(closeResp.getBody(), "UPDATE_NOTE"));
    assertTrue(recordsContainAction(closeResp.getBody(), "CLOSE"));
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
