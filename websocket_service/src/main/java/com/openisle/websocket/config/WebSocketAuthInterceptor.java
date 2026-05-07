package com.openisle.websocket.config;

import com.openisle.websocket.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("WebSocket CONNECT 请求 - 开始认证");
            
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.debug("Authorization 头: {}", authHeader != null ? "存在" : "缺失");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.debug("提取的token长度: {}", token.length());
                
                try {
                    String username = jwtService.extractUsername(token);
                    log.debug("从token中提取的用户名: {}", username);
                    
                    if (username != null && jwtService.isTokenValid(token)) {
                        UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        accessor.setUser(authToken);
                        log.info("WebSocket 连接认证成功，用户: {}", username);
                    } else {
                        log.warn("WebSocket 连接认证失败 - token无效或用户名为空");
                        log.debug("用户名: {}, token有效性: {}", username, jwtService.isTokenValid(token));
                        return null; // 拒绝连接
                    }
                } catch (Exception e) {
                    log.error("WebSocket JWT token处理异常: {}", e.getMessage(), e);
                    return null; // 拒绝连接
                }
            } else {
                log.warn("WebSocket 连接认证失败 - 缺少有效的Authorization头");
                log.debug("Authorization头内容: {}", authHeader);
                return null; // 拒绝连接
            }
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            log.debug("WebSocket SUBSCRIBE 请求到: {}", accessor.getDestination());
        } else if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            log.debug("WebSocket SEND 请求到: {}", accessor.getDestination());
        }
        
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand()) && sent) {
                log.info("WebSocket 连接建立成功");
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                log.info("WebSocket 连接已断开");
            }
        }
    }
}