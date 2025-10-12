package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.dto.JoinRequestMessage;
import com.mahesh.mentee_connect.dto.ApprovalResponseMessage;
import com.mahesh.mentee_connect.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class MeetingWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MeetingService meetingService;

    @MessageMapping("/meeting.join")
    public void handleJoinRequest(@Payload Map<String, String> message) {
        String roomId = message.get("roomId");
        String participantId = message.get("participantId");
        String participantName = message.get("participantName");
        
        try {
            // Get meeting details
            var meeting = meetingService.getMeetingByRoomId(roomId);
            
            // Create join request message
            JoinRequestMessage joinRequest = JoinRequestMessage.builder()
                    .action("join_request")
                    .participantId(participantId)
                    .participantName(participantName)
                    .roomId(roomId)
                    .meetingTitle(meeting.getTitle())
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // Notify mentor about join request
            messagingTemplate.convertAndSend("/topic/meeting/" + roomId + "/join-requests", joinRequest);
        } catch (Exception e) {
            // Send error message back to participant
            messagingTemplate.convertAndSend("/queue/user/" + participantId + "/meeting-error", 
                Map.of("error", "Failed to process join request: " + e.getMessage()));
        }
    }

    @MessageMapping("/meeting.status")
    public void handleMeetingStatusUpdate(@Payload Map<String, String> message) {
        String roomId = message.get("roomId");
        String status = message.get("status");
        String mentorId = message.get("mentorId");
        
        // Broadcast meeting status update to all participants
        messagingTemplate.convertAndSend("/topic/meeting/" + roomId + "/status", 
            Map.of("status", status, "timestamp", LocalDateTime.now().toString()));
    }

    @MessageMapping("/meeting.participant-update")
    public void handleParticipantUpdate(@Payload Map<String, String> message) {
        String roomId = message.get("roomId");
        String participantId = message.get("participantId");
        String action = message.get("action"); // "joined", "left"
        
        // Broadcast participant update to all room subscribers
        messagingTemplate.convertAndSend("/topic/meeting/" + roomId + "/participants", 
            Map.of("participantId", participantId, "action", action, "timestamp", LocalDateTime.now().toString()));
    }
}