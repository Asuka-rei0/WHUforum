package com.openisle.controller;

import com.openisle.dto.NotificationDto;
import com.openisle.dto.NotificationDeliveryPreferenceDto;
import com.openisle.dto.NotificationDeliveryPreferenceUpdateRequest;
import com.openisle.dto.NotificationMarkReadRequest;
import com.openisle.dto.NotificationPreferenceDto;
import com.openisle.dto.NotificationPreferenceUpdateRequest;
import com.openisle.dto.NotificationUnreadCountDto;
import com.openisle.mapper.NotificationMapper;
import com.openisle.service.NotificationService;
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

/** Endpoints for user notifications. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final NotificationMapper notificationMapper;

  @GetMapping
  @Operation(
    summary = "List notifications",
    description = "Retrieve notifications for the current user"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Notifications",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = NotificationDto.class))
    )
  )
  @SecurityRequirement(name = "JWT")
  public List<NotificationDto> list(
    @RequestParam(value = "page", defaultValue = "0") int page,
    @RequestParam(value = "size", defaultValue = "30") int size,
    Authentication auth
  ) {
    return notificationService
      .listNotifications(auth.getName(), null, page, size)
      .stream()
      .map(notificationMapper::toDto)
      .collect(Collectors.toList());
  }

  @GetMapping("/unread")
  @Operation(
    summary = "List unread notifications",
    description = "Retrieve unread notifications for the current user"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Unread notifications",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = NotificationDto.class))
    )
  )
  @SecurityRequirement(name = "JWT")
  public List<NotificationDto> listUnread(
    @RequestParam(value = "page", defaultValue = "0") int page,
    @RequestParam(value = "size", defaultValue = "30") int size,
    Authentication auth
  ) {
    return notificationService
      .listNotifications(auth.getName(), false, page, size)
      .stream()
      .map(notificationMapper::toDto)
      .collect(Collectors.toList());
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Unread count", description = "Get count of unread notifications")
  @ApiResponse(
    responseCode = "200",
    description = "Unread count",
    content = @Content(schema = @Schema(implementation = NotificationUnreadCountDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public NotificationUnreadCountDto unreadCount(Authentication auth) {
    long count = notificationService.countUnread(auth.getName());
    NotificationUnreadCountDto uc = new NotificationUnreadCountDto();
    uc.setCount(count);
    return uc;
  }

  @PostMapping("/read")
  @Operation(summary = "Mark notifications read", description = "Mark notifications as read")
  @ApiResponse(responseCode = "200", description = "Marked read")
  @SecurityRequirement(name = "JWT")
  public void markRead(@RequestBody NotificationMarkReadRequest req, Authentication auth) {
    notificationService.markRead(auth.getName(), req.getIds());
  }

  @DeleteMapping("/read")
  @Operation(summary = "Delete read notifications", description = "Delete read notifications")
  @ApiResponse(responseCode = "200", description = "Read notifications deleted")
  @SecurityRequirement(name = "JWT")
  public void deleteRead(Authentication auth) {
    notificationService.deleteRead(auth.getName());
  }

  @GetMapping("/prefs")
  @Operation(summary = "List preferences", description = "List notification preferences")
  @ApiResponse(
    responseCode = "200",
    description = "Preferences",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = NotificationPreferenceDto.class))
    )
  )
  @SecurityRequirement(name = "JWT")
  public List<NotificationPreferenceDto> prefs(Authentication auth) {
    return notificationService.listPreferences(auth.getName());
  }

  @PostMapping("/prefs")
  @Operation(summary = "Update preference", description = "Update notification preference")
  @ApiResponse(responseCode = "200", description = "Preference updated")
  @SecurityRequirement(name = "JWT")
  public void updatePref(
    @RequestBody NotificationPreferenceUpdateRequest req,
    Authentication auth
  ) {
    notificationService.updatePreference(auth.getName(), req.getType(), req.isEnabled());
  }

  @GetMapping("/email-prefs")
  @Operation(
    summary = "List email preferences",
    description = "List email notification preferences"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Email preferences",
    content = @Content(
      array = @ArraySchema(schema = @Schema(implementation = NotificationPreferenceDto.class))
    )
  )
  @SecurityRequirement(name = "JWT")
  public List<NotificationPreferenceDto> emailPrefs(Authentication auth) {
    return notificationService.listEmailPreferences(auth.getName());
  }

  @PostMapping("/email-prefs")
  @Operation(
    summary = "Update email preference",
    description = "Update email notification preference"
  )
  @ApiResponse(responseCode = "200", description = "Email preference updated")
  @SecurityRequirement(name = "JWT")
  public void updateEmailPref(
    @RequestBody NotificationPreferenceUpdateRequest req,
    Authentication auth
  ) {
    notificationService.updateEmailPreference(auth.getName(), req.getType(), req.isEnabled());
  }

  @GetMapping("/delivery-prefs")
  @Operation(
    summary = "List delivery preferences",
    description = "Get site, email, push and digest notification preferences"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Delivery preferences",
    content = @Content(schema = @Schema(implementation = NotificationDeliveryPreferenceDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public NotificationDeliveryPreferenceDto deliveryPrefs(Authentication auth) {
    return notificationService.getDeliveryPreferences(auth.getName());
  }

  @PostMapping("/delivery-prefs")
  @Operation(
    summary = "Update delivery preferences",
    description = "Update site, email, push and digest notification preferences"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Updated delivery preferences",
    content = @Content(schema = @Schema(implementation = NotificationDeliveryPreferenceDto.class))
  )
  @SecurityRequirement(name = "JWT")
  public NotificationDeliveryPreferenceDto updateDeliveryPrefs(
    @RequestBody NotificationDeliveryPreferenceUpdateRequest request,
    Authentication auth
  ) {
    return notificationService.updateDeliveryPreferences(auth.getName(), request);
  }
}
