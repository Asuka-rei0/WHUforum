package com.openisle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openisle.dto.CasAuthorizeDto;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class WhuCasServiceTest {

  @Test
  void beginAuthorizationBuildsLiveCasLoginUrl() {
    RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    WhuCasService service = new WhuCasService(redisTemplate);
    ReflectionTestUtils.setField(service, "mockEnabled", false);
    ReflectionTestUtils.setField(service, "loginUrl", "https://cas.whu.edu.cn/authserver/login");
    ReflectionTestUtils.setField(service, "serviceUrl", "http://localhost:3000/cas-callback");
    ReflectionTestUtils.setField(service, "mockCampusId", "2024302111348");

    CasAuthorizeDto authorization = service.beginAuthorization(null);
    String prefix = "https://cas.whu.edu.cn/authserver/login?service=";
    String decodedService = URLDecoder.decode(
      authorization.getAuthorizationUrl().substring(prefix.length()),
      StandardCharsets.UTF_8
    );

    assertFalse(authorization.isMock());
    assertTrue(authorization.getAuthorizationUrl().startsWith(prefix));
    assertEquals(
      "http://localhost:3000/cas-callback?state=" + authorization.getState(),
      decodedService
    );
    verify(valueOperations).set(
      eq("whu:cas:state:" + authorization.getState()),
      eq("2024302111348"),
      any(Duration.class)
    );
  }
}
