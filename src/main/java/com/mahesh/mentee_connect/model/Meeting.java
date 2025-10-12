package com.mahesh.mentee_connect.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meetings")
public class Meeting {
    @Id
    private String id;
    
    @DBRef
    private Mentor mentor;
    
    @DBRef
    private Student student;
    
    private String title;
    private String description;
    private LocalDateTime scheduledTime;
    private LocalDateTime endTime;
    private MeetingStatus status;
    private String meetingLink;
    private String location;
    private MeetingType type;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // New fields for enhanced meeting system
    private String roomId;
    private boolean passwordRequired;
    private String password;
    private String joinLink;
    
    public enum MeetingStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
    
    public enum MeetingType {
        ONLINE,
        IN_PERSON
    }
}