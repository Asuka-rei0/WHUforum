package com.openisle.service;

import com.openisle.dto.PostSummaryDto;
import com.openisle.dto.TreeholeAuthorIdentityDto;
import com.openisle.dto.TreeholeInterventionActionRequest;
import com.openisle.dto.TreeholeInterventionCaseDto;
import com.openisle.dto.TreeholeInterventionDetailDto;
import com.openisle.dto.TreeholeInterventionRecordDto;
import com.openisle.exception.FieldException;
import com.openisle.exception.NotFoundException;
import com.openisle.mapper.PostMapper;
import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.PostType;
import com.openisle.model.PostVisibleScopeType;
import com.openisle.model.Role;
import com.openisle.model.TreeholeExpectedVisibility;
import com.openisle.model.TreeholeInterventionAction;
import com.openisle.model.TreeholeInterventionCase;
import com.openisle.model.TreeholeInterventionRecord;
import com.openisle.model.TreeholeInterventionStatus;
import com.openisle.model.TreeholeReviewStatus;
import com.openisle.model.User;
import com.openisle.repository.PostRepository;
import com.openisle.repository.TreeholeInterventionCaseRepository;
import com.openisle.repository.TreeholeInterventionRecordRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TreeholeInterventionService {

  private static final int MAX_NOTE_LENGTH = 1000;

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final TreeholeInterventionCaseRepository caseRepository;
  private final TreeholeInterventionRecordRepository recordRepository;
  private final SearchIndexEventPublisher searchIndexEventPublisher;
  private final PostMapper postMapper;

  @Transactional(noRollbackFor = DataIntegrityViolationException.class)
  public TreeholeInterventionCase ensureCase(Post post, String note) {
    requireTreehole(post);
    return caseRepository
      .findByPost_Id(post.getId())
      .orElseGet(() -> createCaseOrLoadExisting(post, note));
  }

  @Transactional
  public List<TreeholeInterventionCaseDto> listRiskCases(
    com.openisle.model.TreeholeRiskLevel riskLevel,
    TreeholeInterventionStatus status,
    Integer page,
    Integer pageSize
  ) {
    ensureCasesForRiskPosts();
    return caseRepository
      .findRiskCases(riskLevel, status, buildPageable(page, pageSize))
      .stream()
      .map(this::toCaseDto)
      .toList();
  }

  @Transactional
  public TreeholeInterventionDetailDto getDetail(Long postId) {
    TreeholeInterventionCase interventionCase = getOrCreateCaseForPostId(postId);
    TreeholeInterventionDetailDto dto = new TreeholeInterventionDetailDto();
    dto.setCaseInfo(toCaseDto(interventionCase));
    dto.setPost(postMapper.toAdminSummaryDto(interventionCase.getPost()));
    dto.setRecords(
      recordRepository
        .findByInterventionCase_IdOrderByCreatedAtAsc(interventionCase.getId())
        .stream()
        .map(this::toRecordDto)
        .toList()
    );
    return dto;
  }

  @Transactional
  public TreeholeInterventionDetailDto executeAction(
    Long postId,
    TreeholeInterventionActionRequest request,
    String adminUsername
  ) {
    if (request == null || request.getAction() == null) {
      throw new FieldException("action", "Action is required");
    }
    TreeholeInterventionAction action = request.getAction();
    if (
      action == TreeholeInterventionAction.CASE_CREATED ||
      action == TreeholeInterventionAction.VIEW_REAL_IDENTITY
    ) {
      throw new FieldException("action", "Action cannot be executed directly");
    }

    TreeholeInterventionCase interventionCase = getOrCreateCaseForPostId(postId);
    if (interventionCase.getStatus() == TreeholeInterventionStatus.CLOSED) {
      throw new IllegalArgumentException("Treehole intervention case is closed");
    }

    Post post = interventionCase.getPost();
    User admin = getAdmin(adminUsername);
    Snapshot before = Snapshot.capture(interventionCase, post);
    applyAction(interventionCase, post, action, request.getNote());
    Post savedPost = postRepository.save(post);
    TreeholeInterventionCase savedCase = caseRepository.save(interventionCase);
    updateSearchIndex(savedPost);
    recordAction(savedCase, savedPost, admin, action, request.getNote(), before);
    return getDetail(postId);
  }

  @Transactional
  public TreeholeAuthorIdentityDto revealAuthor(Long postId, String reason, String adminUsername) {
    if (StringUtils.isBlank(reason)) {
      throw new FieldException("reason", "Reveal reason is required");
    }
    TreeholeInterventionCase interventionCase = getOrCreateCaseForPostId(postId);
    User admin = getAdmin(adminUsername);
    Post post = interventionCase.getPost();
    Snapshot before = Snapshot.capture(interventionCase, post);
    recordAction(
      interventionCase,
      post,
      admin,
      TreeholeInterventionAction.VIEW_REAL_IDENTITY,
      reason,
      before
    );
    return toAuthorIdentityDto(post.getAuthor());
  }

  @Transactional
  public void recordLegacyAnonymousReveal(Long postId, String adminUsername) {
    Post post = postRepository.findById(postId).orElse(null);
    if (post == null || post.getType() != PostType.TREEHOLE) {
      return;
    }
    TreeholeInterventionCase interventionCase = ensureCase(
      post,
      "Anonymous author resolved from legacy admin endpoint"
    );
    User admin = getAdmin(adminUsername);
    Snapshot before = Snapshot.capture(interventionCase, post);
    recordAction(
      interventionCase,
      post,
      admin,
      TreeholeInterventionAction.VIEW_REAL_IDENTITY,
      "Anonymous author resolved from legacy admin endpoint",
      before
    );
  }

  private void ensureCasesForRiskPosts() {
    postRepository
      .findTreeholeRiskPostsForIntervention()
      .forEach(post -> ensureCase(post, "Treehole risk case created from current review state"));
  }

  private TreeholeInterventionCase getOrCreateCaseForPostId(Long postId) {
    Post post = postRepository
      .findById(postId)
      .orElseThrow(() -> new NotFoundException("Post not found"));
    return ensureCase(post, "Treehole intervention case created by admin access");
  }

  private TreeholeInterventionCase createCaseOrLoadExisting(Post post, String note) {
    try {
      return createCase(post, note);
    } catch (DataIntegrityViolationException ex) {
      return caseRepository.findByPost_Id(post.getId()).orElseThrow(() -> ex);
    }
  }

  private TreeholeInterventionCase createCase(Post post, String note) {
    TreeholeInterventionCase interventionCase = new TreeholeInterventionCase();
    interventionCase.setPost(post);
    interventionCase.setStatus(TreeholeInterventionStatus.OPEN);
    interventionCase.setNote(truncate(note));
    TreeholeInterventionCase saved = caseRepository.saveAndFlush(interventionCase);
    recordAction(
      saved,
      post,
      null,
      TreeholeInterventionAction.CASE_CREATED,
      note,
      Snapshot.empty(post)
    );
    return saved;
  }

  private void applyAction(
    TreeholeInterventionCase interventionCase,
    Post post,
    TreeholeInterventionAction action,
    String note
  ) {
    if (action == TreeholeInterventionAction.MARK_FALSE_POSITIVE) {
      restoreExpectedVisibility(post);
      closeCase(interventionCase, note);
      return;
    }
    if (action == TreeholeInterventionAction.ALLOW_PUBLIC) {
      restoreExpectedVisibility(post);
      closeCase(interventionCase, note);
      return;
    }
    if (action == TreeholeInterventionAction.RESTRICT_PUBLIC) {
      post.setTreeholeReviewStatus(TreeholeReviewStatus.PUBLIC_RESTRICTED);
      post.setStatus(PostStatus.PENDING);
      post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
      interventionCase.setStatus(TreeholeInterventionStatus.IN_PROGRESS);
      updateCaseNote(interventionCase, note);
      return;
    }
    if (action == TreeholeInterventionAction.MARK_CONTACTED) {
      interventionCase.setStatus(TreeholeInterventionStatus.CONTACTED);
      updateCaseNote(interventionCase, note);
      return;
    }
    if (action == TreeholeInterventionAction.MARK_TRANSFERRED) {
      interventionCase.setStatus(TreeholeInterventionStatus.TRANSFERRED);
      updateCaseNote(interventionCase, note);
      return;
    }
    if (action == TreeholeInterventionAction.UPDATE_NOTE) {
      updateCaseNote(interventionCase, note);
      return;
    }
    if (action == TreeholeInterventionAction.CLOSE) {
      closeCase(interventionCase, note);
      return;
    }
    throw new FieldException("action", "Unsupported action");
  }

  private void restoreExpectedVisibility(Post post) {
    if (post.getTreeholeExpectedVisibility() == TreeholeExpectedVisibility.PUBLIC) {
      post.setTreeholeReviewStatus(TreeholeReviewStatus.PUBLIC);
      post.setStatus(PostStatus.PUBLISHED);
      post.setVisibleScope(PostVisibleScopeType.ALL);
      return;
    }
    post.setTreeholeReviewStatus(TreeholeReviewStatus.PRIVATE);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
  }

  private void closeCase(TreeholeInterventionCase interventionCase, String note) {
    interventionCase.setStatus(TreeholeInterventionStatus.CLOSED);
    interventionCase.setClosedAt(LocalDateTime.now());
    updateCaseNote(interventionCase, note);
  }

  private void updateCaseNote(TreeholeInterventionCase interventionCase, String note) {
    if (note != null) {
      interventionCase.setNote(truncate(note));
    }
  }

  private void updateSearchIndex(Post post) {
    if (
      post.getStatus() == PostStatus.PUBLISHED && post.getVisibleScope() == PostVisibleScopeType.ALL
    ) {
      searchIndexEventPublisher.publishPostSaved(post);
      return;
    }
    searchIndexEventPublisher.publishPostDeleted(post.getId());
  }

  private void recordAction(
    TreeholeInterventionCase interventionCase,
    Post post,
    User admin,
    TreeholeInterventionAction action,
    String note,
    Snapshot before
  ) {
    TreeholeInterventionRecord record = new TreeholeInterventionRecord();
    record.setInterventionCase(interventionCase);
    record.setPost(post);
    record.setAdmin(admin);
    record.setAction(action);
    record.setNote(truncate(note));
    record.setFromStatus(before.caseStatus());
    record.setToStatus(interventionCase.getStatus());
    record.setFromReviewStatus(before.reviewStatus());
    record.setToReviewStatus(post.getTreeholeReviewStatus());
    record.setFromPostStatus(before.postStatus());
    record.setToPostStatus(post.getStatus());
    record.setFromVisibleScope(before.visibleScope());
    record.setToVisibleScope(post.getVisibleScope());
    recordRepository.save(record);
  }

  private TreeholeInterventionCaseDto toCaseDto(TreeholeInterventionCase interventionCase) {
    TreeholeInterventionCaseDto dto = new TreeholeInterventionCaseDto();
    dto.setId(interventionCase.getId());
    dto.setPostId(interventionCase.getPost().getId());
    dto.setStatus(interventionCase.getStatus());
    dto.setNote(interventionCase.getNote());
    dto.setCreatedAt(interventionCase.getCreatedAt());
    dto.setUpdatedAt(interventionCase.getUpdatedAt());
    dto.setClosedAt(interventionCase.getClosedAt());
    dto.setPost(postMapper.toAdminSummaryDto(interventionCase.getPost()));
    return dto;
  }

  private TreeholeInterventionRecordDto toRecordDto(TreeholeInterventionRecord record) {
    TreeholeInterventionRecordDto dto = new TreeholeInterventionRecordDto();
    dto.setId(record.getId());
    dto.setCaseId(record.getInterventionCase().getId());
    dto.setPostId(record.getPost().getId());
    dto.setAdminUsername(record.getAdmin() != null ? record.getAdmin().getUsername() : null);
    dto.setAction(record.getAction());
    dto.setNote(record.getNote());
    dto.setFromStatus(record.getFromStatus());
    dto.setToStatus(record.getToStatus());
    dto.setFromReviewStatus(record.getFromReviewStatus());
    dto.setToReviewStatus(record.getToReviewStatus());
    dto.setFromPostStatus(record.getFromPostStatus());
    dto.setToPostStatus(record.getToPostStatus());
    dto.setFromVisibleScope(record.getFromVisibleScope());
    dto.setToVisibleScope(record.getToVisibleScope());
    dto.setCreatedAt(record.getCreatedAt());
    return dto;
  }

  private TreeholeAuthorIdentityDto toAuthorIdentityDto(User user) {
    TreeholeAuthorIdentityDto dto = new TreeholeAuthorIdentityDto();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setCampusIdHash(user.getCampusIdHash());
    dto.setCampusPersonType(
      user.getCampusPersonType() != null ? user.getCampusPersonType().name() : null
    );
    dto.setDepartment(user.getDepartment());
    dto.setCampusVerified(user.isCampusVerified());
    return dto;
  }

  private void requireTreehole(Post post) {
    if (post.getType() != PostType.TREEHOLE) {
      throw new IllegalArgumentException("Post is not a treehole");
    }
  }

  private User getAdmin(String adminUsername) {
    User admin = userRepository
      .findByUsername(adminUsername)
      .orElseThrow(() -> new NotFoundException("Admin not found"));
    if (admin.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("Admin role required");
    }
    return admin;
  }

  private Pageable buildPageable(Integer page, Integer pageSize) {
    int safePage = page == null || page < 0 ? 0 : page;
    int safeSize = pageSize == null || pageSize <= 0 ? 20 : Math.min(pageSize, 100);
    return PageRequest.of(safePage, safeSize);
  }

  private String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= MAX_NOTE_LENGTH ? value : value.substring(0, MAX_NOTE_LENGTH);
  }

  private record Snapshot(
    TreeholeInterventionStatus caseStatus,
    TreeholeReviewStatus reviewStatus,
    PostStatus postStatus,
    PostVisibleScopeType visibleScope
  ) {
    static Snapshot capture(TreeholeInterventionCase interventionCase, Post post) {
      return new Snapshot(
        interventionCase.getStatus(),
        post.getTreeholeReviewStatus(),
        post.getStatus(),
        post.getVisibleScope()
      );
    }

    static Snapshot empty(Post post) {
      return new Snapshot(
        null,
        post.getTreeholeReviewStatus(),
        post.getStatus(),
        post.getVisibleScope()
      );
    }
  }
}
