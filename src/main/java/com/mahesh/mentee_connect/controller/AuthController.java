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
import com.mahesh.mentee_connect.security.jwt.JwtTokenProvider;
import com.mahesh.mentee_connect.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;

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
            if (tokenProvider.validateToken(request.getRefreshToken())) {
                String userId = tokenProvider.getUserIdFromJWT(request.getRefreshToken());
                UserDetails userDetails = authService.loadUserById(userId);
                String newToken = tokenProvider.generateToken(userDetails);
                
                return ResponseEntity.ok(new AuthResponse(
                    newToken, 
                    request.getRefreshToken(),
                    userDetails.getUsername(),
                    userDetails.getAuthorities().iterator().next().getAuthority()
                ));
            }
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid refresh token"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token refresh failed"));
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