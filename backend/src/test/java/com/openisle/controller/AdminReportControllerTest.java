package com.openisle.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openisle.dto.ContentReportActionRequest;
import com.openisle.dto.ContentReportDto;
import com.openisle.model.ContentReport;
import com.openisle.model.ContentReportStatus;
import com.openisle.model.ContentReportTargetType;
import com.openisle.service.ContentReportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ContentReportService contentReportService;

  @Test
  void listReports() throws Exception {
    ContentReport report = new ContentReport();
    report.setId(1L);
    ContentReportDto dto = new ContentReportDto();
    dto.setId(1L);
    dto.setTargetType(ContentReportTargetType.COMMENT);
    dto.setTargetId(2L);
    dto.setStatus(ContentReportStatus.OPEN);

    when(contentReportService.list(ContentReportStatus.OPEN, 0, 50)).thenReturn(List.of(report));
    when(contentReportService.toDto(report)).thenReturn(dto);

    mockMvc
      .perform(get("/api/admin/reports").param("status", "OPEN").param("pageSize", "50"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(1))
      .andExpect(jsonPath("$[0].targetType").value("COMMENT"));
  }

  @Test
  void updateReportStatus() throws Exception {
    ContentReport report = new ContentReport();
    report.setId(3L);
    ContentReportDto dto = new ContentReportDto();
    dto.setId(3L);
    dto.setStatus(ContentReportStatus.RESOLVED);

    when(contentReportService.updateStatus(eq(3L), eq("admin"), any(ContentReportActionRequest.class)))
      .thenReturn(report);
    when(contentReportService.toDto(report)).thenReturn(dto);

    mockMvc
      .perform(
        post("/api/admin/reports/3/actions")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"status\":\"RESOLVED\",\"resolution\":\"done\"}")
          .principal(new UsernamePasswordAuthenticationToken("admin", "p"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(3))
      .andExpect(jsonPath("$.status").value("RESOLVED"));

    verify(contentReportService)
      .updateStatus(eq(3L), eq("admin"), any(ContentReportActionRequest.class));
  }
}
