package com.mahesh.mentee_connect.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Document(collection = "mentor_updates")
public class MentorUpdate {
    
    @Id
    private String id;
    private String title;
    private String content;
    @Indexed
    private String mentorId;
    @Indexed
    private List<String> studentIds = new ArrayList<>(); // List of students who should see this update
    private List<String> attachmentUrls = new ArrayList<>(); // URLs to any attachments (e.g., GitHub links)
    private boolean isImportant;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public MentorUpdate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isImportant = false;
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getMentorId() {
        return mentorId;
    }
    
    public void setMentorId(String mentorId) {
        this.mentorId = mentorId;
    }
    
    public List<String> getStudentIds() {
        return studentIds;
    }
    
    public void setStudentIds(List<String> studentIds) {
        this.studentIds = studentIds;
    }
    
    public List<String> getAttachmentUrls() {
        return attachmentUrls;
    }
    
    public void setAttachmentUrls(List<String> attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }
    
    public boolean isImportant() {
        return isImportant;
    }
    
    public void setImportant(boolean important) {
        isImportant = important;
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