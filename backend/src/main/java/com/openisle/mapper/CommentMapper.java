package com.openisle.mapper;

import com.openisle.dto.CommentDto;
import com.openisle.model.Comment;
import com.openisle.service.CommentService;
import com.openisle.service.ReactionService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Mapper for comments including replies and reactions. */
@Component
@RequiredArgsConstructor
public class CommentMapper {

  private final CommentService commentService;
  private final ReactionService reactionService;
  private final ReactionMapper reactionMapper;
  private final UserMapper userMapper;

  public CommentDto toDto(Comment comment) {
    return toDto(comment, null, false);
  }

  public CommentDto toDto(Comment comment, String viewer, boolean anonymousContext) {
    CommentDto dto = new CommentDto();
    dto.setId(comment.getId());
    dto.setContent(comment.getContent());
    dto.setCreatedAt(comment.getCreatedAt());
    dto.setPinnedAt(comment.getPinnedAt());
    dto.setAnonymous(comment.isAnonymous());
    dto.setAnonymousAlias(comment.getAnonymousAlias());
    dto.setAuthor(
      comment.isAnonymous()
        ? userMapper.toAnonymousAuthorDto(comment.getAnonymousAlias())
        : userMapper.toAuthorDto(comment.getAuthor())
    );
    dto.setReward(0);
    return dto;
  }

  public CommentDto toDtoWithReplies(Comment comment) {
    return toDtoWithReplies(comment, null, false);
  }

  public CommentDto toDtoWithReplies(
    Comment comment,
    String viewer,
    boolean anonymousContext
  ) {
    boolean redactReactions =
      anonymousContext || comment.isAnonymous() || comment.getPost().isAnonymous();
    CommentDto dto = toDto(comment, viewer, anonymousContext);
    dto.setReplies(
      commentService
        .getReplies(comment.getId())
        .stream()
        .map(reply -> toDtoWithReplies(reply, viewer, redactReactions))
        .collect(Collectors.toList())
    );
    dto.setReactions(
      reactionService
        .getReactionsForComment(comment.getId())
        .stream()
        .map(reaction -> reactionMapper.toDto(reaction, viewer, redactReactions))
        .collect(Collectors.toList())
    );
    return dto;
  }
}
