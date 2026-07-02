package com.foodexpress.order.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final byte[] keyBytes;
    private final SecretKey signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtUtil(@Value("${jwt.secret}") String secret) {

        this.keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }


    public Claims parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new MalformedJwtException("JWT must have 3 parts");
            }


            String signingInput = parts[0] + "." + parts[1];
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            String calculatedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            if (!MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.US_ASCII),
                    parts[2].getBytes(StandardCharsets.US_ASCII))) {
                throw new MalformedJwtException("JWT signature validation failed");
            }


            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claimsMap = objectMapper.readValue(payloadBytes, Map.class);


            if (claimsMap.containsKey("exp")) {
                Number exp = (Number) claimsMap.get("exp");
                if (System.currentTimeMillis() / 1000 > exp.longValue()) {
                    throw new ExpiredJwtException(null, null, "JWT expired");
                }
            }


            String issuer = (String) claimsMap.get("iss");
            if (!"foodexpress-auth".equals(issuer)) {
                throw new MalformedJwtException("Invalid JWT issuer");
            }


            return Jwts.claims().add(claimsMap).build();

        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new MalformedJwtException("Failed to parse JWT", e);
        }
    }


    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }


    public Long getUserId(Claims claims) {
        Object userIdObj = claims.get("user_id");
        if (userIdObj == null) {

            userIdObj = claims.getSubject();
        }
        if (userIdObj == null) {
            throw new IllegalArgumentException("JWT has no user_id or sub claim");
        }
        
        String userIdStr = userIdObj.toString();
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric user_id format: " + userIdStr);
        }
    }


    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }


    public UUID getRestaurantId(Claims claims) {
        Object rid = claims.get("restaurant_id");
        if (rid != null) {
            try {
                return UUID.fromString(rid.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID restaurant_id in token: {}", rid);
            }
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public List<String> getPermissions(Claims claims) {
        Object perms = claims.get("permissions");
        if (perms instanceof List<?> list) {
            return list.stream()
                    .map(item -> {
                        if (item instanceof String s) return s;
                        if (item instanceof Map<?, ?> map) {
                            Object code = map.get("code");
                            return code != null ? code.toString() : null;
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        return Collections.emptyList();
    }
}
