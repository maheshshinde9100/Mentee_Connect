package com.mahesh.mentee_connect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/websocket-test")
@Tag(name = "WebSocket Test", description = "WebSocket testing endpoints")
public class WebSocketTestController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send-join-request")
    @Operation(summary = "Test join request", description = "Send a test join request notification")
    public Map<String, String> testJoinRequest(@RequestBody Map<String, String> request) {
        String roomId = request.get("roomId");
        String participantId = request.get("participantId");
        String participantName = request.get("participantName");
        
        Map<String, Object> message = Map.of(
            "action", "join_request",
            "participantId", participantId,
            "participantName", participantName,
            "roomId", roomId,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/meeting/" + roomId + "/join-requests", message);
        
        return Map.of("status", "sent", "message", "Join request sent to room " + roomId);
    }

    @PostMapping("/send-approval")
    @Operation(summary = "Test approval", description = "Send a test approval notification")
    public Map<String, String> testApproval(@RequestBody Map<String, String> request) {
        String participantId = request.get("participantId");
        String roomId = request.get("roomId");
        
        Map<String, Object> message = Map.of(
            "status", "approved",
            "roomId", roomId,
            "meetingTitle", "Test Meeting",
            "message", "You have been approved to join the meeting"
        );
        
        messagingTemplate.convertAndSend("/queue/user/" + participantId + "/meeting-approval", message);
        
        return Map.of("status", "sent", "message", "Approval sent to user " + participantId);
    }
}