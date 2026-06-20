package com.openisle.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.openisle.dto.ContentReportDto;
import com.openisle.model.ContentReport;
import com.openisle.model.ContentReportTargetType;
import com.openisle.model.Message;
import com.openisle.model.MessageConversation;
import org.junit.jupiter.api.Test;

class ContentReportMapperTest {

  private final ContentReportMapper mapper = new ContentReportMapper();

  @Test
  void mapsMessageConversationId() {
    MessageConversation conversation = new MessageConversation();
    conversation.setId(88L);

    Message message = new Message();
    message.setId(12L);
    message.setContent("hello");
    message.setConversation(conversation);

    ContentReport report = new ContentReport();
    report.setId(1L);
    report.setTargetType(ContentReportTargetType.MESSAGE);
    report.setTargetId(12L);
    report.setMessage(message);

    ContentReportDto dto = mapper.toDto(report);

    assertEquals(12L, dto.getMessageId());
    assertEquals(88L, dto.getConversationId());
  }
}
