// 1. Video Call Entity (src/main/java/com/mahesh/mentee_connect/entity/VideoCall.java)
package com.mahesh.mentee_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Document(collection = "video_calls")
public class VideoCall {
    @Id
    private String id;
    private String mentorId;
    private String mentorName;
    private String roomId;
    private String joinLink; // New field for shareable link
    private String title;
    private String description;
    private LocalDateTime scheduledTime;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status; // SCHEDULED, WAITING_FOR_HOST, LIVE, ENDED, CANCELLED
    private List<String> participants = new ArrayList<>();
    private List<String> waitingParticipants = new ArrayList<>(); // New field for waiting room
    private Integer maxParticipants;
    private String meetingPassword;
    private boolean requiresApproval = true; // New field for host approval

    // Constructors
    public VideoCall() {
        this.createdAt = LocalDateTime.now();
        this.status = "SCHEDULED";
    }

    public VideoCall(String mentorId, String mentorName, String title, String description, LocalDateTime scheduledTime) {
        this();
        this.mentorId = mentorId;
        this.mentorName = mentorName;
        this.title = title;
        this.description = description;
        this.scheduledTime = scheduledTime;
        this.roomId = generateRoomId();
        this.joinLink = generateJoinLink();
    }

    private String generateRoomId() {
        return "room_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    private String generateJoinLink() {
        return "/meeting/join/" + this.roomId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public String getMeetingPassword() { return meetingPassword; }
    public void setMeetingPassword(String meetingPassword) { this.meetingPassword = meetingPassword; }

    public String getJoinLink() { return joinLink; }
    public void setJoinLink(String joinLink) { this.joinLink = joinLink; }

    public List<String> getWaitingParticipants() { return waitingParticipants; }
    public void setWaitingParticipants(List<String> waitingParticipants) { this.waitingParticipants = waitingParticipants; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
}
