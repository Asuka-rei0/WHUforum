package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.exception.RateLimitException;
import com.openisle.model.Post;
import com.openisle.model.User;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.CommentSubscriptionRepository;
import com.openisle.repository.NotificationRepository;
import com.openisle.repository.PointHistoryRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import com.openisle.service.PointService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommentServiceTest {

  @Test
  void addCommentRespectsRateLimit() {
    CommentRepository commentRepo = mock(CommentRepository.class);
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    CommentSubscriptionRepository subRepo = mock(CommentSubscriptionRepository.class);
    NotificationRepository nRepo = mock(NotificationRepository.class);
    PointHistoryRepository pointHistoryRepo = mock(PointHistoryRepository.class);
    PointService pointService = mock(PointService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);

    CommentService service = new CommentService(
      commentRepo,
      postRepo,
      userRepo,
      notifService,
      subService,
      reactionRepo,
      subRepo,
      nRepo,
      pointHistoryRepo,
      pointService,
      imageUploader,
      searchIndexEventPublisher,
      mock(AnonymousAuditService.class),
      mock(ModerationService.class)
    );

    when(commentRepo.countByAuthorAfter(eq("alice"), any())).thenReturn(3L);

    assertThrows(RateLimitException.class, () -> service.addComment("alice", 1L, "hi"));
  }

  @Test
  void deleteAllByPostHardRemovesCommentDependenciesIncludingSoftDeletedRows() {
    CommentRepository commentRepo = mock(CommentRepository.class);
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    CommentSubscriptionRepository subRepo = mock(CommentSubscriptionRepository.class);
    NotificationRepository nRepo = mock(NotificationRepository.class);
    PointHistoryRepository pointHistoryRepo = mock(PointHistoryRepository.class);
    PointService pointService = mock(PointService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);
    AnonymousAuditService anonymousAuditService = mock(AnonymousAuditService.class);

    CommentService service = new CommentService(
      commentRepo,
      postRepo,
      userRepo,
      notifService,
      subService,
      reactionRepo,
      subRepo,
      nRepo,
      pointHistoryRepo,
      pointService,
      imageUploader,
      searchIndexEventPublisher,
      anonymousAuditService,
      mock(ModerationService.class)
    );

    Post post = new Post();
    post.setId(7L);
    User affectedUser = new User();
    affectedUser.setId(11L);
    List<Long> commentIds = List.of(1L, 2L);

    when(commentRepo.findAllIdsByPostIdIncludingDeleted(7L)).thenReturn(commentIds);
    when(commentRepo.findAllContentsByPostIdIncludingDeleted(7L)).thenReturn(List.of("old", "new"));
    when(pointHistoryRepo.findDistinctUsersByCommentIds(commentIds)).thenReturn(
      List.of(affectedUser)
    );
    when(pointService.recalculateUserPoints(affectedUser)).thenReturn(42);

    service.deleteAllByPostHard(post);

    verify(reactionRepo).deleteByComment_IdIn(commentIds);
    verify(subRepo).deleteByComment_IdIn(commentIds);
    verify(nRepo).deleteByComment_IdIn(commentIds);
    verify(anonymousAuditService).deleteByCommentIds(commentIds);
    verify(pointHistoryRepo).markDeletedAndDetachComments(eq(commentIds), any(LocalDateTime.class));
    verify(commentRepo).clearParentReferences(commentIds);
    verify(commentRepo).hardDeleteByIds(commentIds);
    verify(searchIndexEventPublisher).publishCommentDeleted(1L);
    verify(searchIndexEventPublisher).publishCommentDeleted(2L);
    verify(userRepo).saveAll(any());
  }
}
