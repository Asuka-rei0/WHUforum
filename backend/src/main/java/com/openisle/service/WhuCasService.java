package com.openisle.service;

import com.openisle.dto.CasAuthorizeDto;
import com.openisle.model.CampusPersonType;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class WhuCasService {

  private static final String STATE_PREFIX = "whu:cas:state:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${app.whu.cas.mock-enabled:false}")
  private boolean mockEnabled;

  @Value("${app.whu.cas.login-url:https://cas.whu.edu.cn/authserver/login}")
  private String loginUrl;

  @Value("${app.whu.cas.validate-url:https://cas.whu.edu.cn/authserver/serviceValidate}")
  private String validateUrl;

  @Value("${app.whu.cas.service-url:http://localhost:3000/cas-callback}")
  private String serviceUrl;

  @Value("${app.whu.cas.mock-campus-id:2024302111348}")
  private String mockCampusId;

  @Value("${app.whu.cas.mock-display-name:WHU User}")
  private String mockDisplayName;

  @Value("${app.whu.cas.mock-department:Wuhan University}")
  private String mockDepartment;

  @Value("${app.whu.cas.attribute-campus-id:uid}")
  private String campusIdAttribute;

  @Value("${app.whu.cas.attribute-display-name:cn}")
  private String displayNameAttribute;

  @Value("${app.whu.cas.attribute-email:mail}")
  private String emailAttribute;

  @Value("${app.whu.cas.attribute-person-type:personType}")
  private String personTypeAttribute;

  @Value("${app.whu.cas.attribute-department:department}")
  private String departmentAttribute;

  public WhuCasService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public CasAuthorizeDto beginAuthorization(String requestedCampusId) {
    String state = UUID.randomUUID().toString();
    String campusId = requestedCampusId == null || requestedCampusId.isBlank()
      ? mockCampusId
      : requestedCampusId.trim();
    redisTemplate.opsForValue().set(STATE_PREFIX + state, campusId, Duration.ofMinutes(10));

    CasAuthorizeDto dto = new CasAuthorizeDto();
    dto.setState(state);
    dto.setMock(mockEnabled);
    if (mockEnabled) {
      dto.setAuthorizationUrl(
        UriComponentsBuilder.fromUriString(serviceUrl)
          .queryParam("state", state)
          .queryParam("ticket", "mock-" + campusId)
          .build()
          .toUriString()
      );
    } else {
      String service = serviceUrlWithState(state);
      dto.setAuthorizationUrl(
        loginUrl + "?service=" + URLEncoder.encode(service, StandardCharsets.UTF_8)
      );
    }
    return dto;
  }

  public WhuCasProfile validate(String state, String ticket) {
    if (state == null || state.isBlank() || ticket == null || ticket.isBlank()) {
      throw new IllegalArgumentException("CAS state and ticket are required");
    }
    String key = STATE_PREFIX + state;
    Object value = redisTemplate.opsForValue().get(key);
    if (value == null) {
      throw new IllegalArgumentException("Invalid or expired CAS state");
    }
    redisTemplate.delete(key);
    if (mockEnabled) {
      String campusId = String.valueOf(value);
      if (ticket.startsWith("mock-") && ticket.length() > "mock-".length()) {
        campusId = ticket.substring("mock-".length()).trim();
      }
      return buildMockProfile(campusId);
    }
    return validateLiveTicket(state, ticket);
  }

  private WhuCasProfile validateLiveTicket(String state, String ticket) {
    String url = UriComponentsBuilder.fromUriString(validateUrl)
      .queryParam("ticket", ticket)
      .queryParam("service", serviceUrlWithState(state))
      .build()
      .toUriString();
    try {
      String xml = restTemplate.getForObject(url, String.class);
      return parseCasProfile(xml);
    } catch (RestClientException e) {
      throw new IllegalStateException("Unable to validate WHU CAS ticket", e);
    }
  }

  private String serviceUrlWithState(String state) {
    return UriComponentsBuilder.fromUriString(serviceUrl)
      .queryParam("state", state)
      .build()
      .toUriString();
  }

  private WhuCasProfile parseCasProfile(String xml) {
    if (xml == null || xml.isBlank()) {
      throw new IllegalArgumentException("Empty WHU CAS validation response");
    }
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      Document document = factory
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
      Element success = firstElement(document, "authenticationSuccess");
      if (success == null) {
        throw new IllegalArgumentException("Invalid WHU CAS ticket");
      }
      String subject = firstText(success, "user");
      if (subject == null || subject.isBlank()) {
        throw new IllegalArgumentException("WHU CAS response has no subject");
      }
      String campusId = firstNonBlank(firstText(success, campusIdAttribute), subject);
      String displayName = firstNonBlank(firstText(success, displayNameAttribute), subject);
      String email = firstText(success, emailAttribute);
      String department = firstText(success, departmentAttribute);
      CampusPersonType personType = parsePersonType(firstText(success, personTypeAttribute));
      return new WhuCasProfile(subject, campusId, displayName, email, personType, department);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to parse WHU CAS validation response", e);
    }
  }

  private Element firstElement(Document document, String localName) {
    NodeList nodes = document.getElementsByTagNameNS("*", localName);
    if (nodes.getLength() == 0) {
      nodes = document.getElementsByTagName("cas:" + localName);
    }
    return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
  }

  private String firstText(Element root, String localName) {
    if (localName == null || localName.isBlank()) {
      return null;
    }
    NodeList nodes = root.getElementsByTagNameNS("*", localName);
    if (nodes.getLength() == 0) {
      nodes = root.getElementsByTagName("cas:" + localName);
    }
    if (nodes.getLength() == 0) {
      nodes = root.getElementsByTagName(localName);
    }
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private CampusPersonType parsePersonType(String raw) {
    if (raw == null || raw.isBlank()) {
      return CampusPersonType.UNKNOWN;
    }
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    if (normalized.contains("student") || normalized.contains("\u5b66\u751f")) {
      return CampusPersonType.STUDENT;
    }
    if (normalized.contains("faculty") || normalized.contains("\u6559\u5e08")) {
      return CampusPersonType.FACULTY;
    }
    if (normalized.contains("staff") || normalized.contains("\u804c\u5de5")) {
      return CampusPersonType.STAFF;
    }
    if (normalized.contains("alumni") || normalized.contains("\u6821\u53cb")) {
      return CampusPersonType.ALUMNI;
    }
    try {
      return CampusPersonType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return CampusPersonType.UNKNOWN;
    }
  }

  private WhuCasProfile buildMockProfile(String campusId) {
    String normalized = campusId == null || campusId.isBlank() ? mockCampusId : campusId.trim();
    CampusPersonType type = normalized.length() >= 10
      ? CampusPersonType.STUDENT
      : CampusPersonType.UNKNOWN;
    String username = ("whu" + normalized).toLowerCase(Locale.ROOT);
    return new WhuCasProfile(
      "mock:" + normalized,
      normalized,
      firstNonBlank(mockDisplayName, username),
      username + "@whu.edu.cn",
      type,
      mockDepartment
    );
  }
}
