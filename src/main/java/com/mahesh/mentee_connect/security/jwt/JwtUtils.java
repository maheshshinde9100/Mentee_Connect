package com.mahesh.mentee_connect.security.jwt;

import java.util.Date;
import java.security.Key;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.mahesh.mentee_connect.security.services.UserDetailsImpl;
import io.jsonwebtoken.io.Decoders;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    public int getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    private volatile Key cachedSigningKey;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        String role = userPrincipal.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        String subject;
        String rawUsername = userPrincipal.getRawUsername();
        if (rawUsername != null && !rawUsername.trim().isEmpty()) {
            subject = rawUsername;
        } else {
            subject = userPrincipal.getEmail() != null ? userPrincipal.getEmail() : userPrincipal.getUsername();
        }

        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .claim("userId", userPrincipal.getId())
                .claim("username", rawUsername)
                .claim("email", userPrincipal.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS384)
                .compact();
    }

    public String generateTokenFromUserDetails(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        String userId = "";
        String rawUsername = null;
        String email = null;
        if (userDetails instanceof UserDetailsImpl) {
            UserDetailsImpl impl = (UserDetailsImpl) userDetails;
            userId = impl.getId();
            rawUsername = impl.getRawUsername();
            email = impl.getEmail();
        }

        String subject;
        if (rawUsername != null && !rawUsername.trim().isEmpty()) {
            subject = rawUsername;
        } else {
            subject = email != null ? email : userDetails.getUsername();
        }

        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .claim("userId", userId)
                .claim("username", rawUsername)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS384)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error parsing JWT claims: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getUserNameFromJwtToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        return claims.getSubject();
    }

    public String getEmailFromJwtToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        return claims.get("email", String.class);
    }

    public String getUsernameClaimFromJwtToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        return claims.get("username", String.class);
    }

    public String getUserIdFromJWT(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isEmpty()) {
            return claims.getSubject();
        }
        return userId;
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage(), e);
        } catch (io.jsonwebtoken.security.SecurityException e) {
            logger.error("JWT security violation: {}", e.getMessage(), e);
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token structure: {}", e.getMessage(), e);
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage(), e);
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage(), e);
        } catch (ClaimJwtException e) {
            logger.error("JWT claim validation failed: {}", e.getMessage(), e);
        } catch (JwtException e) {
            logger.error("General JWT validation failure: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during JWT validation: {}", e.getMessage(), e);
        }
        return false;
    }

    public String getRoleFromJwtToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            logger.error("Error getting role from JWT token: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getUserIdFromJwtToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("userId", String.class);
        } catch (Exception e) {
            logger.error("Error getting userId from JWT token: {}", e.getMessage(), e);
            return null;
        }
    }

    private Key getSigningKey() {
        if (cachedSigningKey != null) {
            return cachedSigningKey;
        }
        try {
            byte[] keyBytes;
            String secret = jwtSecret == null ? "" : jwtSecret.trim();
            if (secret.isEmpty()) {
                throw new IllegalStateException("JWT secret is not configured. Set JWT_SECRET env var or app.jwtSecret property.");
            }
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (Exception e) {
                try {
                    keyBytes = hexStringToByteArray(secret);
                } catch (Exception e2) {
                    logger.error("JWT secret is not valid Base64 or Hex. Length={}", secret.length());
                    throw e2;
                }
                logger.info("JWT secret decoded from HEX string ({} bytes). Recommended: use Base64 next time.", keyBytes.length);
            }
            Key key = Keys.hmacShaKeyFor(keyBytes);
            cachedSigningKey = key;
            return key;
        } catch (Exception e) {
            logger.error("Failed to build JWT signing key: {}", e.getMessage(), e);
            throw new IllegalStateException("JWT signing key could not be created", e);
        }
    }

    public static byte[] hexStringToByteArrayStatic(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex character at position " + i);
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }

    private byte[] hexStringToByteArray(String s) {
        return hexStringToByteArrayStatic(s);
    }
}
