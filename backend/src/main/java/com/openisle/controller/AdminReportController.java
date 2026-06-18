package com.openisle.controller;

import com.openisle.dto.ContentReportActionRequest;
import com.openisle.dto.ContentReportDto;
import com.openisle.model.ContentReportStatus;
import com.openisle.service.ContentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class AdminReportController {

  private final ContentReportService contentReportService;

  @GetMapping
  @Operation(summary = "List content reports", description = "List user-submitted moderation reports")
  @ApiResponse(
    responseCode = "200",
    description = "Reports",
    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContentReportDto.class)))
  )
  public List<ContentReportDto> list(
    @RequestParam(value = "status", required = false) ContentReportStatus status,
    @RequestParam(value = "page", defaultValue = "0") int page,
    @RequestParam(value = "pageSize", defaultValue = "30") int pageSize
  ) {
    return contentReportService
      .list(status, page, pageSize)
      .stream()
      .map(contentReportService::toDto)
      .toList();
  }

  @PostMapping("/{id}/actions")
  @Operation(summary = "Handle content report", description = "Update report status and resolution")
  @ApiResponse(
    responseCode = "200",
    description = "Updated report",
    content = @Content(schema = @Schema(implementation = ContentReportDto.class))
  )
  public ResponseEntity<ContentReportDto> action(
    @PathVariable Long id,
    @RequestBody ContentReportActionRequest request,
    Authentication auth
  ) {
    return ResponseEntity.ok(
      contentReportService.toDto(contentReportService.updateStatus(id, auth.getName(), request))
    );
  }
}
