package com.mahesh.mentee_connect.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProgressReport {
    private String id;
    private String studentId;
    private String studentName;
    private LocalDateTime reportDate;
    private double attendance;
    private double cgpa;
    private double taskCompletionRate;
    private double recentAttendanceRate;
    private String mentorComments;
    private String academicProgress;
}