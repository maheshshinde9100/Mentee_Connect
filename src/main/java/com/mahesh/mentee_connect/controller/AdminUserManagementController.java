package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.Admin;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.payload.response.UserResponse;
import com.mahesh.mentee_connect.payload.request.UpdateProfileRequest;
import com.mahesh.mentee_connect.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminUserManagementController {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserManagementController.class);

    @Autowired
    private UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        logger.info("Fetching all users");
        List<User> users = userManagementService.getAllUsers();
        List<UserResponse> response = users.stream()
            .map(this::convertToUserResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String name) {
        logger.info("Searching users with name: {}", name);
        List<User> users = userManagementService.searchUsersByName(name);
        List<UserResponse> response = users.stream()
            .map(this::convertToUserResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        try {
            logger.info("Attempting to delete user");
            userManagementService.deleteUser(userId);
            return ResponseEntity.ok().body(new MessageResponse("User deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Error deleting user: " + e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        try {
            logger.info("Attempting to update user");
            User updatedUser = userManagementService.updateUser(userId, updateRequest);
            return ResponseEntity.ok(convertToUserResponse(updatedUser));
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Error updating user: " + e.getMessage()));
        }
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phoneNumber(user.getPhoneNumber())
            .role(user.getRole());

        if (user instanceof Student) {
            Student student = (Student) user;
            builder.specificId(student.getStudentId())
                   .additionalInfo(student.getCourse());
        } else if (user instanceof Mentor) {
            Mentor mentor = (Mentor) user;
            builder.specificId(mentor.getMentorId())
                   .additionalInfo(mentor.getDepartment());
        } else if (user instanceof Admin) {
            Admin admin = (Admin) user;
            builder.specificId(admin.getAdminId())
                   .additionalInfo(admin.getDepartment());
        }

        return builder.build();
    }
}

class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 