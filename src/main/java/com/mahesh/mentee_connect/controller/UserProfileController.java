package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Admin;
import com.mahesh.mentee_connect.payload.response.UserResponse;
import com.mahesh.mentee_connect.payload.request.UpdateProfileRequest;
import com.mahesh.mentee_connect.service.AuthService;
import com.mahesh.mentee_connect.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/profile")
@CrossOrigin(origins = {"http://localhost:3000", "https://menteeconnect.vercel.app"})
public class UserProfileController {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserManagementService userManagementService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        logger.info("Fetching profile for current user");
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            logger.error("No authenticated user found");
            return ResponseEntity.badRequest().build();
        }

        UserResponse userResponse = convertToUserResponse(currentUser);
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUserProfile(@Valid @RequestBody UpdateProfileRequest updateRequest) {
        logger.info("Updating profile for current user");
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                logger.error("No authenticated user found");
                return ResponseEntity.badRequest().body("No authenticated user found");
            }

            // Validate email uniqueness if it's being changed
            if (!currentUser.getEmail().equals(updateRequest.getEmail())) {
                if (userManagementService.isEmailTaken(updateRequest.getEmail())) {
                    return ResponseEntity.badRequest().body("Email is already in use");
                }
            }

            // Update the user details
            currentUser.setFirstName(updateRequest.getFirstName());
            currentUser.setLastName(updateRequest.getLastName());
            currentUser.setEmail(updateRequest.getEmail());
            currentUser.setPhoneNumber(updateRequest.getPhoneNumber());

            // Update role-specific fields
            if (currentUser instanceof Student) {
                Student student = (Student) currentUser;
                if (updateRequest.getCourse() != null) {
                    student.setCourse(updateRequest.getCourse());
                }
                if (updateRequest.getBatch() != null) {
                    student.setBatch(updateRequest.getBatch());
                }
                if (updateRequest.getSemester() != null) {
                    student.setSemester(updateRequest.getSemester());
                }
            } else if (currentUser instanceof Mentor) {
                Mentor mentor = (Mentor) currentUser;
                if (updateRequest.getDepartment() != null) {
                    mentor.setDepartment(updateRequest.getDepartment());
                }
            } else if (currentUser instanceof Admin) {
                Admin admin = (Admin) currentUser;
                if (updateRequest.getDepartment() != null) {
                    admin.setDepartment(updateRequest.getDepartment());
                }
                if (updateRequest.getPosition() != null) {
                    admin.setPosition(updateRequest.getPosition());
                }
            }

            // Save the updated user
            User updatedUser = userManagementService.updateUser(currentUser.getId(), updateRequest);
            logger.info("Profile updated successfully for user: {}", updatedUser.getEmail());

            return ResponseEntity.ok(convertToUserResponse(updatedUser));
        } catch (Exception e) {
            logger.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
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
                   .additionalInfo(String.format("Course: %s, Semester: %d, Batch: %s", 
                                               student.getCourse(), 
                                               student.getSemester(), 
                                               student.getBatch()));
        } else if (user instanceof Mentor) {
            Mentor mentor = (Mentor) user;
            builder.specificId(mentor.getMentorId())
                   .additionalInfo(String.format("Department: %s", mentor.getDepartment()));
        } else if (user instanceof Admin) {
            Admin admin = (Admin) user;
            builder.specificId(admin.getAdminId())
                   .additionalInfo(String.format("Department: %s, Position: %s", 
                                               admin.getDepartment(), 
                                               admin.getPosition()));
        }

        return builder.build();
    }
}