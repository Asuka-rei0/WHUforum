package com.openisle.mapper;

import com.openisle.dto.ReactionDto;
import com.openisle.model.Reaction;
import org.springframework.stereotype.Component;

/** Mapper for reactions. */
@Component
public class ReactionMapper {

  public ReactionDto toDto(Reaction reaction) {
    return toDto(reaction, null, false);
  }

  public ReactionDto toDto(Reaction reaction, String viewer, boolean anonymousContext) {
    ReactionDto dto = new ReactionDto();
    dto.setId(reaction.getId());
    dto.setType(reaction.getType());
    String username = reaction.getUser().getUsername();
    dto.setUser(anonymousContext && !username.equals(viewer) ? null : username);
    if (reaction.getPost() != null) {
      dto.setPostId(reaction.getPost().getId());
    }
    if (reaction.getComment() != null) {
      dto.setCommentId(reaction.getComment().getId());
    }
    if (reaction.getMessage() != null) {
      dto.setMessageId(reaction.getMessage().getId());
    }
    dto.setReward(0);
    return dto;
  }
}
