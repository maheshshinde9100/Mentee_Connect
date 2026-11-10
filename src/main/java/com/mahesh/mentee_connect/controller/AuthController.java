package com.mahesh.mentee_connect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.mahesh.mentee_connect.payload.request.LoginRequest;
import com.mahesh.mentee_connect.payload.request.RefreshTokenRequest;
import com.mahesh.mentee_connect.payload.request.RegisterRequest;
import com.mahesh.mentee_connect.payload.response.AuthResponse;
import com.mahesh.mentee_connect.payload.response.ErrorResponse;
import com.mahesh.mentee_connect.payload.response.JwtResponse;
import com.mahesh.mentee_connect.security.jwt.JwtUtils;
import com.mahesh.mentee_connect.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtils jwtUtils;

    @Operation(summary = "Login user", description = "Authenticate a user and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register user", description = "Create a new student or mentor account and return a JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully registered",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        JwtResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            logger.debug("Refresh token request received");

            if (refreshToken == null || refreshToken.isBlank()) {
                logger.warn("Refresh token is missing or empty");
                return ResponseEntity.badRequest().body(new ErrorResponse("Refresh token is required"));
            }

            if (!jwtUtils.validateJwtToken(refreshToken)) {
                logger.warn("Refresh token validation failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid or expired refresh token"));
            }

            String userId = jwtUtils.getUserIdFromJWT(refreshToken);
            String emailClaim = jwtUtils.getEmailFromJwtToken(refreshToken);
            String usernameClaim = jwtUtils.getUsernameClaimFromJwtToken(refreshToken);
            String subject = jwtUtils.getUserNameFromJwtToken(refreshToken);

            logger.debug("Refresh token claims — userId='{}', email='{}', username='{}', subject='{}'",
                    userId, emailClaim, usernameClaim, subject);

            UserDetails userDetails = null;
            String lookup = null;
            if (userId != null && !userId.isBlank()) {
                try {
                    lookup = "userId:" + userId;
                    userDetails = authService.loadUserById(userId);
                } catch (Exception e1) {
                    logger.warn("loadUserById({}) failed: {}", userId, e1.getMessage());
                }
            }
            if (userDetails == null) {
                String identifier = emailClaim != null && !emailClaim.isBlank() ? emailClaim
                        : (usernameClaim != null && !usernameClaim.isBlank() ? usernameClaim : subject);
                if (identifier != null && !identifier.isBlank()) {
                    try {
                        lookup = "identifier:" + identifier;
                        userDetails = authService.loadUserByUsername(identifier);
                    } catch (Exception e2) {
                        logger.warn("loadUserByUsername({}) failed: {}", identifier, e2.getMessage());
                    }
                }
            }

            if (userDetails == null) {
                logger.error("Refresh token valid but user not found by any claim. userId='{}', email='{}', username='{}', subject='{}'",
                        userId, emailClaim, usernameClaim, subject);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid refresh token: user not found"));
            }

            String newToken = jwtUtils.generateTokenFromUserDetails(userDetails);
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority())
                    .orElse("");
            String userDbId = userDetails instanceof com.mahesh.mentee_connect.security.services.UserDetailsImpl
                    ? ((com.mahesh.mentee_connect.security.services.UserDetailsImpl) userDetails).getId()
                    : "";
            String userEmail = userDetails instanceof com.mahesh.mentee_connect.security.services.UserDetailsImpl
                    ? ((com.mahesh.mentee_connect.security.services.UserDetailsImpl) userDetails).getEmail()
                    : "";

            logger.info("Refresh token succeeded via {} -> userId={} role={}", lookup, userDbId, role);
            return ResponseEntity.ok(new JwtResponse(
                    newToken,
                    refreshToken,
                    userDbId,
                    userDetails.getUsername(),
                    userEmail,
                    role.isEmpty() ? java.util.List.of() : java.util.List.of(role),
                    (int) (jwtUtils.getJwtExpirationMs() / 1000)
            ));
        } catch (Exception ex) {
            logger.error("Token refresh failed with exception: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token refresh failed: " + ex.getMessage()));
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<java.util.Map<String, String>> ping() {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("status", "UP");
        response.put("message", "MenteeConnect backend is warm and running!");
        return ResponseEntity.ok(response);
    }
}