package com.openisle.websocket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    
    @Value("${app.website-url}")
    private String websiteUrl;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix("wss-heartbeat-thread-");
        ts.initialize();

        config.enableSimpleBroker("/queue", "/topic")
              .setHeartbeatValue(new long[]{10000, 10000})
              .setTaskScheduler(ts);
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1) 原生 WebSocket（不带 SockJS）
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns(
                        "https://staging.open-isle.com",
                        "https://www.staging.open-isle.com",
                        websiteUrl,
                        websiteUrl.replace("://www.", "://"),
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://192.168.7.98:*",
                        "http://30.211.97.238:*"
                );

        // 2) SockJS 回退：单独路径
        registry.addEndpoint("/api/sockjs")
                .setAllowedOriginPatterns(
                        "https://staging.open-isle.com",
                        "https://www.staging.open-isle.com",
                        websiteUrl,
                        websiteUrl.replace("://www.", "://"),
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://192.168.7.98:*",
                        "http://30.211.97.238:*"
                )
                .withSockJS()
                .setWebSocketEnabled(true)
                .setSessionCookieNeeded(false);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}