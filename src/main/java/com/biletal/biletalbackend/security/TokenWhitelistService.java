package com.biletal.biletalbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenWhitelistService {
    // Whitelist implementation: Map token to user ID for quick lookup
    private final Map<String, Long> validTokens = new ConcurrentHashMap<>();
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    /**
     * Add token to the whitelist
     */
    public void addToken(String token, Long userId) {
        validTokens.put(token, userId);
    }
    
    /**
     * Remove token from whitelist
     */
    public void removeToken(String token) {
        validTokens.remove(token);
    }
    
    /**
     * Remove all tokens for a specific user
     */
    public void removeAllUserTokens(Long userId) {
        validTokens.entrySet().removeIf(entry -> entry.getValue().equals(userId));
    }
    
    /**
     * Check if token is in whitelist (valid)
     */
    public boolean isTokenValid(String token) {
        if(!validTokens.containsKey(token)) {
            return false;
        }

        try {
            // check expire time finished
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();

            if(claims.getExpiration().before(new Date())){
                removeToken(token);
                return false;
            }

            return true;
        } catch (Exception e) {
            // If token parsing fails (malformed, expired, etc.), remove it and return false
            removeToken(token);
            return false;
        }
    }
    
    /**
     * Cleanup expired tokens from the whitelist
     * Runs automatically every day at midnight
     */
    @Scheduled(cron = "0 0 */1 * * ?") // Run at midnight every day
    public void removeExpiredTokens() {
        validTokens.keySet().removeIf(token -> {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey.getBytes())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                
                return claims.getExpiration().before(new Date());
            } catch (Exception e) {
                // If token can't be parsed, it's invalid
                return true;
            }
        });
    }
}
