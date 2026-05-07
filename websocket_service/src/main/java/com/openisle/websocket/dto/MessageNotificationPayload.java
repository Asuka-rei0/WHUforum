package com.openisle.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageNotificationPayload implements Serializable {
    private String targetUsername;
    private Object payload;
}