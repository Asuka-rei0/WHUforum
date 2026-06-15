package com.openisle.controller;

import com.openisle.dto.TreeholeAuthorIdentityDto;
import com.openisle.dto.TreeholeInterventionActionRequest;
import com.openisle.dto.TreeholeInterventionCaseDto;
import com.openisle.dto.TreeholeInterventionDetailDto;
import com.openisle.dto.TreeholeRevealAuthorRequest;
import com.openisle.model.TreeholeInterventionStatus;
import com.openisle.model.TreeholeRiskLevel;
import com.openisle.service.TreeholeInterventionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/treeholes")
@RequiredArgsConstructor
public class AdminTreeholeController {

  private final TreeholeInterventionService treeholeInterventionService;

  @GetMapping("/risk")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "List risk treeholes", description = "List treeholes requiring intervention")
  @ApiResponse(
    responseCode = "200",
    description = "Risk treehole cases",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = TreeholeInterventionCaseDto.class))
    )
  )
  public List<TreeholeInterventionCaseDto> riskCases(
    @RequestParam(value = "page", required = false) Integer page,
    @RequestParam(value = "pageSize", required = false) Integer pageSize,
    @RequestParam(value = "riskLevel", required = false) TreeholeRiskLevel riskLevel,
    @RequestParam(value = "status", required = false) TreeholeInterventionStatus status
  ) {
    return treeholeInterventionService.listRiskCases(riskLevel, status, page, pageSize);
  }

  @GetMapping("/{postId}")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Treehole intervention detail", description = "Get intervention detail")
  @ApiResponse(
    responseCode = "200",
    description = "Treehole intervention detail",
    content = @Content(schema = @Schema(implementation = TreeholeInterventionDetailDto.class))
  )
  public TreeholeInterventionDetailDto detail(@PathVariable Long postId) {
    return treeholeInterventionService.getDetail(postId);
  }

  @PostMapping("/{postId}/actions")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Execute treehole intervention action")
  @ApiResponse(
    responseCode = "200",
    description = "Updated intervention detail",
    content = @Content(schema = @Schema(implementation = TreeholeInterventionDetailDto.class))
  )
  public TreeholeInterventionDetailDto action(
    @PathVariable Long postId,
    @RequestBody TreeholeInterventionActionRequest request,
    Authentication auth
  ) {
    return treeholeInterventionService.executeAction(postId, request, auth.getName());
  }

  @PostMapping("/{postId}/reveal-author")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Reveal treehole author identity")
  @ApiResponse(
    responseCode = "200",
    description = "Real author identity",
    content = @Content(schema = @Schema(implementation = TreeholeAuthorIdentityDto.class))
  )
  public TreeholeAuthorIdentityDto revealAuthor(
    @PathVariable Long postId,
    @RequestBody TreeholeRevealAuthorRequest request,
    Authentication auth
  ) {
    return treeholeInterventionService.revealAuthor(postId, request.getReason(), auth.getName());
  }
}
