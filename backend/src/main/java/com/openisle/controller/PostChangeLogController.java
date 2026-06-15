package com.openisle.controller;

import com.openisle.dto.PostChangeLogDto;
import com.openisle.mapper.PostChangeLogMapper;
import com.openisle.model.Post;
import com.openisle.service.PostChangeLogService;
import com.openisle.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class PostChangeLogController {

  private final PostChangeLogService changeLogService;
  private final PostChangeLogMapper mapper;
  private final PostService postService;

  @GetMapping("/{id}/change-logs")
  @Operation(summary = "Post change logs", description = "List change logs for a post")
  @ApiResponse(
    responseCode = "200",
    description = "Change logs",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostChangeLogDto.class))
    )
  )
  public List<PostChangeLogDto> listLogs(@PathVariable Long id, Authentication auth) {
    String viewer = auth != null ? auth.getName() : null;
    Post post = postService.getViewablePost(id, viewer);
    return changeLogService
      .listLogs(id)
      .stream()
      .map(log -> mapper.toDto(log, post.isAnonymous()))
      .collect(Collectors.toList());
  }
}
