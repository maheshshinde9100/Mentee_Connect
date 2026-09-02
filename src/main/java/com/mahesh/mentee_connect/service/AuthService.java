package com.mahesh.mentee_connect.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.mahesh.mentee_connect.payload.request.LoginRequest;
import com.mahesh.mentee_connect.payload.response.JwtResponse;
import com.mahesh.mentee_connect.model.User;

public interface AuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    UserDetails loadUserById(String userId);
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    User getCurrentUser();
    String getCurrentUserId();
    boolean isCurrentUserAdmin();
    boolean isCurrentUserMentor();
    boolean isCurrentUserStudent();
}