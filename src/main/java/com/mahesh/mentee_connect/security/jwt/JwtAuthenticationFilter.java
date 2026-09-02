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
                boolean valid = jwtUtils.validateJwtToken(jwt);
                if (valid) {
                    String usernameOrEmail = jwtUtils.getUserNameFromJwtToken(jwt);
                    String userId = jwtUtils.getUserIdFromJwtToken(jwt);

                    if (usernameOrEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails;
                        try {
                            userDetails = userDetailsService.loadUserByUsername(usernameOrEmail);
                        } catch (Exception lookupEx) {
                            if (userId != null) {
                                try {
                                    userDetails = userDetailsService.loadUserById(userId);
                                    logger.info("JWT subject lookup failed for '{}'; fell back to userId={}", usernameOrEmail, userId);
                                } catch (Exception fallbackEx) {
                                    logger.warn("Could not load user from JWT (subject='{}', userId={}): {}",
                                            usernameOrEmail, userId, fallbackEx.getMessage());
                                    filterChain.doFilter(request, response);
                                    return;
                                }
                            } else {
                                logger.warn("JWT subject lookup failed for '{}' and no userId claim: {}",
                                        usernameOrEmail, lookupEx.getMessage());
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Authenticated user '{}' for URI {}", usernameOrEmail, requestURI);
                        }
                    }
                } else {
                    logger.warn("Rejected invalid JWT for request: {}", requestURI);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication for URI {}: {}", requestURI, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
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