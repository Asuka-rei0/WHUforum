package com.openisle.service;

import com.openisle.dto.AnonymousAuditDto;
import com.openisle.model.AnonymousAudit;
import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.User;
import com.openisle.repository.AnonymousAuditRepository;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnonymousAuditService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private final AnonymousAuditRepository anonymousAuditRepository;

  public String createAlias() {
    return "珞珈匿名" + (1000 + RANDOM.nextInt(9000));
  }

  public void recordPost(Post post, User user, String alias, String reason) {
    AnonymousAudit audit = new AnonymousAudit();
    audit.setPost(post);
    audit.setUser(user);
    audit.setAlias(alias);
    audit.setReason(reason);
    anonymousAuditRepository.save(audit);
  }

  public void recordComment(Comment comment, User user, String alias, String reason) {
    AnonymousAudit audit = new AnonymousAudit();
    audit.setPost(comment.getPost());
    audit.setComment(comment);
    audit.setUser(user);
    audit.setAlias(alias);
    audit.setReason(reason);
    anonymousAuditRepository.save(audit);
  }

  public List<AnonymousAuditDto> listByPost(Long postId) {
    return anonymousAuditRepository
      .findByPost_IdOrderByCreatedAtAsc(postId)
      .stream()
      .map(this::toDto)
      .toList();
  }

  private AnonymousAuditDto toDto(AnonymousAudit audit) {
    AnonymousAuditDto dto = new AnonymousAuditDto();
    dto.setId(audit.getId());
    dto.setPostId(audit.getPost() != null ? audit.getPost().getId() : null);
    dto.setCommentId(audit.getComment() != null ? audit.getComment().getId() : null);
    dto.setAlias(audit.getAlias());
    dto.setRealUsername(audit.getUser().getUsername());
    dto.setCampusIdHash(audit.getUser().getCampusIdHash());
    dto.setCampusPersonType(
      audit.getUser().getCampusPersonType() != null
        ? audit.getUser().getCampusPersonType().name()
        : null
    );
    dto.setDepartment(audit.getUser().getDepartment());
    dto.setReason(audit.getReason());
    dto.setCreatedAt(audit.getCreatedAt());
    return dto;
  }
}
