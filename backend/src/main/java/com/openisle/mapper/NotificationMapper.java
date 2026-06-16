package com.openisle.mapper;

import com.openisle.dto.NotificationDto;
import com.openisle.dto.PostSummaryDto;
import com.openisle.model.Comment;
import com.openisle.model.Notification;
import com.openisle.model.Post;
import com.openisle.model.Role;
import com.openisle.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Mapper for notifications. */
@Component
@RequiredArgsConstructor
public class NotificationMapper {

  private final CommentMapper commentMapper;
  private final UserMapper userMapper;

  public NotificationDto toDto(Notification n) {
    NotificationDto dto = new NotificationDto();
    boolean anonymousContext = isAnonymousContext(n);
    dto.setId(n.getId());
    dto.setType(n.getType());
    if (n.getPost() != null) {
      PostSummaryDto postDto = new PostSummaryDto();
      postDto.setId(n.getPost().getId());
      postDto.setTitle(
        anonymousContext && !canReceiveAnonymousDetails(n) ? "匿名内容" : n.getPost().getTitle()
      );
      dto.setPost(postDto);
    }
    if (n.getComment() != null) {
      dto.setComment(commentMapper.toDto(n.getComment(), null, anonymousContext));
      Comment parent = n.getComment().getParent();
      if (parent != null) {
        dto.setParentComment(commentMapper.toDto(parent, null, anonymousContext));
      }
    }
    if (n.getFromUser() != null && !anonymousContext) {
      dto.setFromUser(userMapper.toAuthorDto(n.getFromUser()));
    }
    if (n.getReactionType() != null) {
      dto.setReactionType(n.getReactionType());
    }
    dto.setApproved(n.getApproved());
    dto.setContent(n.getContent());
    dto.setRead(n.isRead());
    dto.setCreatedAt(n.getCreatedAt());
    return dto;
  }

  private boolean isAnonymousContext(Notification n) {
    if (n.getPost() != null && n.getPost().isAnonymous()) {
      return true;
    }
    if (n.getComment() == null) {
      return false;
    }
    Post post = n.getComment().getPost();
    return n.getComment().isAnonymous() || (post != null && post.isAnonymous());
  }

  private boolean canReceiveAnonymousDetails(Notification n) {
    User recipient = n.getUser();
    Post post = n.getPost();
    if (post == null && n.getComment() != null) {
      post = n.getComment().getPost();
    }
    return (
      recipient != null &&
      (
        recipient.getRole() == Role.ADMIN ||
        (post != null && post.getAuthor() != null && recipient.getId().equals(post.getAuthor().getId()))
      )
    );
  }
}
