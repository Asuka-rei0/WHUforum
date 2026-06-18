package com.openisle.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openisle.dto.ContentReportDto;
import com.openisle.dto.ContentReportRequest;
import com.openisle.model.ContentReport;
import com.openisle.model.ContentReportReason;
import com.openisle.model.ContentReportStatus;
import com.openisle.model.ContentReportTargetType;
import com.openisle.service.ContentReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContentReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContentReportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ContentReportService contentReportService;

  @Test
  void createReport() throws Exception {
    ContentReport report = new ContentReport();
    report.setId(12L);

    ContentReportDto dto = new ContentReportDto();
    dto.setId(12L);
    dto.setTargetType(ContentReportTargetType.POST);
    dto.setTargetId(34L);
    dto.setReason(ContentReportReason.SPAM);
    dto.setStatus(ContentReportStatus.OPEN);

    when(contentReportService.create(eq("alice"), any(ContentReportRequest.class)))
      .thenReturn(report);
    when(contentReportService.toDto(report)).thenReturn(dto);

    mockMvc
      .perform(
        post("/api/reports")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"targetType\":\"POST\",\"targetId\":34,\"reason\":\"SPAM\"}")
          .principal(new UsernamePasswordAuthenticationToken("alice", "p"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(12))
      .andExpect(jsonPath("$.targetType").value("POST"))
      .andExpect(jsonPath("$.status").value("OPEN"));

    verify(contentReportService).create(eq("alice"), any(ContentReportRequest.class));
  }
}
