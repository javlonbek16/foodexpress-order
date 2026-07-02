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

/**
 * Utility class for parsing and validating JWTs locally.
 * Extracts user ID, role, and permissions from the token claims.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final byte[] keyBytes;
    private final SecretKey signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Use raw key bytes — same as Python's jwt.decode() with the raw secret string.
        // Do NOT pad or transform the key, it must match Auth service byte-for-byte.
        this.keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Parse and validate the JWT, returning the claims.
     * Performs manual signature verification to support keys under 256 bits (JJWT 0.12+ strict check bypass).
     */
    public Claims parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new MalformedJwtException("JWT must have 3 parts");
            }

            // Verify signature manually using HMAC-SHA256
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

            // Parse payload
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claimsMap = objectMapper.readValue(payloadBytes, Map.class);

            // Verify expiration
            if (claimsMap.containsKey("exp")) {
                Number exp = (Number) claimsMap.get("exp");
                if (System.currentTimeMillis() / 1000 > exp.longValue()) {
                    throw new ExpiredJwtException(null, null, "JWT expired");
                }
            }

            // Verify issuer (to match issuer='foodexpress-auth' in Python/Django)
            String issuer = (String) claimsMap.get("iss");
            if (!"foodexpress-auth".equals(issuer)) {
                throw new MalformedJwtException("Invalid JWT issuer");
            }

            // Build Claims object using JJWT builder
            return Jwts.claims().add(claimsMap).build();

        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new MalformedJwtException("Failed to parse JWT", e);
        }
    }

    /**
     * Validate the token without returning claims.
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract the user ID from the 'user_id' claim.
     * NestJS auth service puts user ID in 'user_id', not 'sub'.
     */
    public Long getUserId(Claims claims) {
        Object userIdObj = claims.get("user_id");
        if (userIdObj == null) {
            // fallback to subject if present
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

    /**
     * Extract the role from the token (e.g., CUSTOMER, RESTAURANT, COURIER, ADMIN).
     */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    /**
     * Extract the restaurant_id claim (for RESTAURANT role users).
     */
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

    /**
     * Extract the permissions list from the token.
     * NestJS auth sends permissions as [{id, code}] objects — we extract the 'code' field.
     */
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
