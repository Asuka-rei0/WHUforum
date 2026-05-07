package com.openisle.websocket.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> new User(username, "", Collections.emptyList());
    }
}