package com.mahesh.mentee_connect.model;

import lombok.Data;
import lombok.AllArgsConstructor;
@Data
@AllArgsConstructor
public class StudentProgressStats {
    private String studentId;
    private String studentName;
    private String status;
    private double attendance;
    private double cgpa;
}