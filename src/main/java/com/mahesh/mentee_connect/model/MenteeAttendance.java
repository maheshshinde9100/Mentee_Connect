package com.mahesh.mentee_connect.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import java.util.Map;
import java.util.HashMap;

@Document(collection = "mentee_attendance")
public class MenteeAttendance {
    
    @Id
    private String id;
    @Field(targetType = FieldType.OBJECT_ID)
    @Indexed
    private String mentorId;
    @Indexed
    private LocalDate date;
    @Indexed
    private String batchId;
    private String sessionTopic;
    private Map<String, AttendanceStatus> studentAttendance = new HashMap<>(); // Map of studentId to attendance status
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LATE,
        EXCUSED
    }
    
    // Constructors
    public MenteeAttendance() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getMentorId() {
        return mentorId;
    }
    
    public void setMentorId(String mentorId) {
        this.mentorId = mentorId;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public String getBatchId() {
        return batchId;
    }
    
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
    
    public String getSessionTopic() {
        return sessionTopic;
    }
    
    public void setSessionTopic(String sessionTopic) {
        this.sessionTopic = sessionTopic;
    }
    
    public Map<String, AttendanceStatus> getStudentAttendance() {
        return studentAttendance;
    }
    
    public void setStudentAttendance(Map<String, AttendanceStatus> studentAttendance) {
        this.studentAttendance = studentAttendance;
    }
    
    public void markAttendance(String studentId, AttendanceStatus status) {
        this.studentAttendance.put(studentId, status);
    }
    
    public AttendanceStatus getStudentStatus(String studentId) {
        return this.studentAttendance.getOrDefault(studentId, null);
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 