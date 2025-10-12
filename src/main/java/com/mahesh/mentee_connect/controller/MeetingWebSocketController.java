package com.mahesh.mentee_connect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class MeetingWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/meeting.join")
    public void handleJoinRequest(@Payload Map<String, String> message) {
        String roomId = message.get("roomId");
        String participantId = message.get("participantId");
        String participantName = message.get("participantName");
        
        // Notify mentor about join request
        messagingTemplate.convertAndSend("/topic/meeting/" + roomId + "/join-requests", 
            Map.of("participantId", participantId, "participantName", participantName, "action", "join_request"));
    }

    @MessageMapping("/meeting.approve")
    public void handleApproval(@Payload Map<String, String> message) {
        String roomId = message.get("roomId");
        String participantId = message.get("participantId");
        
        // Notify participant about approval
        messagingTemplate.convertAndSend("/queue/user/" + participantId + "/meeting-approval", 
            Map.of("roomId", roomId, "status", "approved"));
    }

    @MessageMapping("/meeting.reject")
    public void handleRejection(@Payload Map<String, String> message) {
        String roomId = message.get("roomId");
        String participantId = message.get("participantId");
        
        // Notify participant about rejection
        messagingTemplate.convertAndSend("/queue/user/" + participantId + "/meeting-approval", 
            Map.of("roomId", roomId, "status", "rejected"));
    }
}