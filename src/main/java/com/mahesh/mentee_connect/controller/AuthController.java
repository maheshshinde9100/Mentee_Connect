package com.mahesh.mentee_connect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.mahesh.mentee_connect.payload.request.LoginRequest;
import com.mahesh.mentee_connect.payload.request.RefreshTokenRequest;
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

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            logger.info("Refresh token request received");
            
            if (refreshToken == null || refreshToken.isBlank()) {
                logger.warn("Refresh token is missing or empty");
                return ResponseEntity.badRequest().body(new ErrorResponse("Refresh token is required"));
            }

            if (jwtUtils.validateJwtToken(refreshToken)) {
                String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
                if (username == null || username.isBlank()) {
                    logger.error("Refresh token valid but username could not be extracted");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ErrorResponse("Invalid refresh token: no username"));
                }

                UserDetails userDetails = authService.loadUserById(username);
                String newToken = jwtUtils.generateTokenFromUserDetails(userDetails);
                String role = userDetails.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority())
                        .orElse("");

                logger.info("Refresh token succeeded for user: {}", username);
                return ResponseEntity.ok(new AuthResponse(
                    newToken,
                    refreshToken,
                    userDetails.getUsername(),
                    role
                ));
            }

            logger.warn("Refresh token validation failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid or expired refresh token"));
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