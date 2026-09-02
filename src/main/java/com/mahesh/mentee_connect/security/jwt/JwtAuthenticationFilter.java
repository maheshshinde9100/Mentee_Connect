package com.mahesh.mentee_connect.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.mahesh.mentee_connect.security.services.UserDetailsServiceImpl;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String connection = request.getHeader("Connection");
        String upgrade = request.getHeader("Upgrade");
        if (connection != null && connection.toLowerCase().contains("upgrade") &&
            upgrade != null && upgrade.toLowerCase().equals("websocket")) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();

        try {
            String jwt = parseJwt(request);

            if (jwt != null) {
                logger.debug("JWT token found for request: {}", requestURI);
                if (jwtUtils.validateJwtToken(jwt)) {
                    UserDetails userDetails = loadUserFromJwtClaims(jwt);
                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Successfully authenticated user: {} for request: {}", userDetails.getUsername(), requestURI);
                    } else {
                        logger.warn("JWT token valid but user details could not be loaded for request: {}", requestURI);
                    }
                } else {
                    logger.warn("JWT token validation FAILED for request: {}", requestURI);
                }
            } else {
                logger.debug("No JWT token found in Authorization header for request: {}", requestURI);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication for request {}: {}", requestURI, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private UserDetails loadUserFromJwtClaims(String jwt) {
        String userId = jwtUtils.getUserIdFromJWT(jwt);
        if (userId != null && !userId.trim().isEmpty()) {
            try {
                UserDetails ud = userDetailsService.loadUserById(userId);
                if (ud != null) {
                    logger.debug("Loaded user by userId claim: {}", userId);
                    return ud;
                }
            } catch (Exception e) {
                logger.warn("Failed to load user by userId claim {}: {}", userId, e.getMessage());
            }
        }

        String email = jwtUtils.getEmailFromJwtToken(jwt);
        if (email != null && !email.trim().isEmpty()) {
            try {
                UserDetails ud = userDetailsService.loadUserByUsername(email);
                if (ud != null) {
                    logger.debug("Loaded user by email claim: {}", email);
                    return ud;
                }
            } catch (Exception e) {
                logger.warn("Failed to load user by email claim {}: {}", email, e.getMessage());
            }
        }

        String usernameClaim = jwtUtils.getUsernameClaimFromJwtToken(jwt);
        if (usernameClaim != null && !usernameClaim.trim().isEmpty()) {
            try {
                UserDetails ud = userDetailsService.loadUserByUsername(usernameClaim);
                if (ud != null) {
                    logger.debug("Loaded user by username claim: {}", usernameClaim);
                    return ud;
                }
            } catch (Exception e) {
                logger.warn("Failed to load user by username claim {}: {}", usernameClaim, e.getMessage());
            }
        }

        String subject = jwtUtils.getUserNameFromJwtToken(jwt);
        if (subject != null && !subject.trim().isEmpty()) {
            try {
                UserDetails ud = userDetailsService.loadUserByUsername(subject);
                if (ud != null) {
                    logger.debug("Loaded user by JWT subject: {}", subject);
                    return ud;
                }
            } catch (Exception e) {
                logger.warn("Failed to load user by subject {}: {}", subject, e.getMessage());
            }
        }

        logger.error("Unable to load user from JWT using any available claim");
        return null;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            return token;
        }

        return null;
    }
}