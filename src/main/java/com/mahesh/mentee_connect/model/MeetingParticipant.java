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
@Document(collection = "meeting_participants")
public class MeetingParticipant {
    @Id
    private String id;
    
    private String meetingId;
    private String roomId;
    
    @DBRef
    private User participant;
    
    private ParticipantStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    private String responseMessage;
    
    public enum ParticipantStatus {
        WAITING,
        APPROVED,
        REJECTED,
        JOINED,
        LEFT
    }
}