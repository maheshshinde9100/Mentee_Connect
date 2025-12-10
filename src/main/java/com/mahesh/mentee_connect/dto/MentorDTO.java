package com.mahesh.mentee_connect.dto;

import com.mahesh.mentee_connect.model.User.UserRole;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class MentorDTO {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private String mentorId;
    private String department;
    private String specialization;
    private String designation;
    private int yearsOfExperience;
    
    // Only include student IDs, not the entire student objects
    private List<String> assignedStudentIds = new ArrayList<>();
    
    private int maxStudents;
} 