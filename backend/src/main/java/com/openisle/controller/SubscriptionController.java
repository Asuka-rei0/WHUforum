package com.openisle.controller;

import com.openisle.model.Comment;
import com.openisle.service.CommentService;
import com.openisle.service.PostService;
import com.openisle.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Endpoints for subscribing to posts, comments and users. */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

  private final SubscriptionService subscriptionService;
  private final PostService postService;
  private final CommentService commentService;

  @PostMapping("/posts/{postId}")
  @Operation(summary = "Subscribe post", description = "Subscribe to a post")
  @ApiResponse(responseCode = "200", description = "Subscribed")
  @SecurityRequirement(name = "JWT")
  public void subscribePost(@PathVariable Long postId, Authentication auth) {
    postService.getViewablePost(postId, auth.getName());
    subscriptionService.subscribePost(auth.getName(), postId);
  }

  @DeleteMapping("/posts/{postId}")
  @Operation(summary = "Unsubscribe post", description = "Unsubscribe from a post")
  @ApiResponse(responseCode = "200", description = "Unsubscribed")
  @SecurityRequirement(name = "JWT")
  public void unsubscribePost(@PathVariable Long postId, Authentication auth) {
    postService.getViewablePost(postId, auth.getName());
    subscriptionService.unsubscribePost(auth.getName(), postId);
  }

  @PostMapping("/comments/{commentId}")
  @Operation(summary = "Subscribe comment", description = "Subscribe to a comment")
  @ApiResponse(responseCode = "200", description = "Subscribed")
  @SecurityRequirement(name = "JWT")
  public void subscribeComment(@PathVariable Long commentId, Authentication auth) {
    Comment comment = commentService.getComment(commentId);
    postService.getViewablePost(comment.getPost().getId(), auth.getName());
    subscriptionService.subscribeComment(auth.getName(), commentId);
  }

  @DeleteMapping("/comments/{commentId}")
  @Operation(summary = "Unsubscribe comment", description = "Unsubscribe from a comment")
  @ApiResponse(responseCode = "200", description = "Unsubscribed")
  @SecurityRequirement(name = "JWT")
  public void unsubscribeComment(@PathVariable Long commentId, Authentication auth) {
    Comment comment = commentService.getComment(commentId);
    postService.getViewablePost(comment.getPost().getId(), auth.getName());
    subscriptionService.unsubscribeComment(auth.getName(), commentId);
  }

  @PostMapping("/users/{username}")
  @Operation(summary = "Subscribe user", description = "Subscribe to a user")
  @ApiResponse(responseCode = "200", description = "Subscribed")
  @SecurityRequirement(name = "JWT")
  public void subscribeUser(@PathVariable String username, Authentication auth) {
    subscriptionService.subscribeUser(auth.getName(), username);
  }

  @DeleteMapping("/users/{username}")
  @Operation(summary = "Unsubscribe user", description = "Unsubscribe from a user")
  @ApiResponse(responseCode = "200", description = "Unsubscribed")
  @SecurityRequirement(name = "JWT")
  public void unsubscribeUser(@PathVariable String username, Authentication auth) {
    subscriptionService.unsubscribeUser(auth.getName(), username);
  }
}
