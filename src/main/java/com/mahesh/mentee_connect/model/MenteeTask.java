package com.mahesh.mentee_connect.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "mentee_tasks")
public class MenteeTask {
    
    @Id
    private String id;
    private String title;
    private String description;
    @Field(targetType = FieldType.OBJECT_ID)
    @Indexed
    private String mentorId;
    @Field(targetType = FieldType.OBJECT_ID)
    @Indexed
    private String studentId;
    @Indexed
    private LocalDate dueDate;
    @Indexed
    private TaskStatus status;
    private String feedback;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum TaskStatus {
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        OVERDUE
    }
    
    // Constructors
    public MenteeTask() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = TaskStatus.ASSIGNED;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMentorId() {
        return mentorId;
    }
    
    public void setMentorId(String mentorId) {
        this.mentorId = mentorId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
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