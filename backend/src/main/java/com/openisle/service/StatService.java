package com.openisle.service;

import com.openisle.repository.CommentRepository;
import com.openisle.repository.ContentReportRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.TreeholeInterventionCaseRepository;
import com.openisle.repository.UserRepository;
import com.openisle.dto.AdminDashboardStatsDto;
import com.openisle.model.ContentReportStatus;
import com.openisle.model.PostStatus;
import com.openisle.model.TreeholeInterventionStatus;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatService {

  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final ContentReportRepository contentReportRepository;
  private final TreeholeInterventionCaseRepository treeholeInterventionCaseRepository;

  private Map<LocalDate, Long> toDateMap(
    LocalDate start,
    LocalDate end,
    java.util.List<Object[]> list
  ) {
    Map<LocalDate, Long> result = new LinkedHashMap<>();
    for (var obj : list) {
      Object dateObj = obj[0];
      LocalDate d;
      if (dateObj instanceof java.sql.Date sqlDate) {
        d = sqlDate.toLocalDate();
      } else if (dateObj instanceof LocalDate localDate) {
        d = localDate;
      } else {
        d = LocalDate.parse(dateObj.toString());
      }
      Long c = ((Number) obj[1]).longValue();
      result.put(d, c);
    }
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      result.putIfAbsent(d, 0L);
    }
    return result;
  }

  public Map<LocalDate, Long> countNewUsersRange(LocalDate start, LocalDate end) {
    java.util.List<Object[]> list = userRepository.countDailyRange(
      start.atStartOfDay(),
      end.plusDays(1).atStartOfDay()
    );
    return toDateMap(start, end, list);
  }

  public Map<LocalDate, Long> countPostsRange(LocalDate start, LocalDate end) {
    java.util.List<Object[]> list = postRepository.countDailyRange(
      start.atStartOfDay(),
      end.plusDays(1).atStartOfDay()
    );
    return toDateMap(start, end, list);
  }

  public Map<LocalDate, Long> countCommentsRange(LocalDate start, LocalDate end) {
    java.util.List<Object[]> list = commentRepository.countDailyRange(
      start.atStartOfDay(),
      end.plusDays(1).atStartOfDay()
    );
    return toDateMap(start, end, list);
  }

  public AdminDashboardStatsDto adminDashboardStats() {
    AdminDashboardStatsDto dto = new AdminDashboardStatsDto();
    dto.setUsers(userRepository.count());
    dto.setPosts(postRepository.count());
    dto.setComments(commentRepository.count());
    dto.setOpenReports(contentReportRepository.countByStatus(ContentReportStatus.OPEN));
    dto.setReviewingReports(contentReportRepository.countByStatus(ContentReportStatus.REVIEWING));
    dto.setPendingPosts(postRepository.countByStatus(PostStatus.PENDING));
    dto.setOpenTreeholeCases(
      treeholeInterventionCaseRepository.countByStatus(TreeholeInterventionStatus.OPEN)
    );
    return dto;
  }
}
