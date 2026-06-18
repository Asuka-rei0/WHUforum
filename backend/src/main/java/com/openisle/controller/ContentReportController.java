package com.openisle.controller;

import com.openisle.dto.ContentReportDto;
import com.openisle.dto.ContentReportRequest;
import com.openisle.service.ContentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class ContentReportController {

  private final ContentReportService contentReportService;

  @PostMapping
  @Operation(summary = "Create content report", description = "Report a post, comment, message or treehole")
  @ApiResponse(
    responseCode = "200",
    description = "Created report",
    content = @Content(schema = @Schema(implementation = ContentReportDto.class))
  )
  public ResponseEntity<ContentReportDto> create(
    @RequestBody ContentReportRequest request,
    Authentication auth
  ) {
    return ResponseEntity.ok(contentReportService.toDto(contentReportService.create(auth.getName(), request)));
  }
}
