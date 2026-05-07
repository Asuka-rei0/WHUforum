package com.openisle.websocket.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret}")
    private String secret;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        logger.debug("解析JWT token - secret长度: {}", secret != null ? secret.length() : "null");
        
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("JWT解析失败: {}", e.getMessage());
            throw e;
        }
    }

    private Key getSignInKey() {
        // 使用与backend相同的密钥处理方式：直接Base64解码
        byte[] keyBytes;
        try {
            // 尝试Base64解码
            keyBytes = java.util.Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // 如果不是Base64格式，使用UTF-8字节
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            // 确保密钥长度至少256位（32字节）
            if (keyBytes.length < 32) {
                MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("SHA-256");
                    keyBytes = digest.digest(keyBytes);
                } catch (NoSuchAlgorithmException ex) {
                    throw new IllegalStateException("SHA-256 not available", ex);
                }
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String validateAndGetSubject(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}