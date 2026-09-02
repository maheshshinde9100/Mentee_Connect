package com.mahesh.mentee_connect.payload.response;

import lombok.Data;
import java.util.List;

@Data
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn = 86400;
    private String id;
    private String username;
    private String email;
    private List<String> roles;

    public JwtResponse(String token, String refreshToken, String id, String username, String email, List<String> roles) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public JwtResponse(String token, String refreshToken, String id, String username, String email, List<String> roles, long expiresInSec) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.expiresIn = expiresInSec;
    }

    public JwtResponse(String token, String id, String username, String email, List<String> roles) {
        this(token, token, id, username, email, roles);
    }
} 