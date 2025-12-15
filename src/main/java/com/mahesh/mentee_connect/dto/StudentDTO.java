package com.mahesh.mentee_connect.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String rollNumber;
    private String branch;
    private Double cgpa;
    private String mentorId;
    private String mentorName;
    private String phoneNumber;
} 