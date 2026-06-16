package com.openisle.controller;

import com.openisle.dto.PostSummaryDto;
import com.openisle.mapper.PostMapper;
import com.openisle.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/treeholes")
@RequiredArgsConstructor
public class TreeholeController {

  private final PostService postService;
  private final PostMapper postMapper;

  @GetMapping("/square")
  @Operation(summary = "Treehole square", description = "List public treeholes")
  @ApiResponse(
    responseCode = "200",
    description = "Treehole square posts",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostSummaryDto.class))
    )
  )
  public List<PostSummaryDto> square(
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    Authentication auth
  ) {
    String viewer = auth != null ? auth.getName() : null;
    return postMapper.toListDtos(postService.listTreeholeSquare(viewer, page, pageSize));
  }

  @GetMapping("/me")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "My treeholes", description = "List current user's treeholes")
  @ApiResponse(
    responseCode = "200",
    description = "Current user's treehole posts",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = PostSummaryDto.class))
    )
  )
  public ResponseEntity<List<PostSummaryDto>> me(
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    Authentication auth
  ) {
    if (auth == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
    }
    return ResponseEntity.ok(
      postMapper.toListDtos(postService.listMyTreeholes(auth.getName(), page, pageSize))
    );
  }
}
