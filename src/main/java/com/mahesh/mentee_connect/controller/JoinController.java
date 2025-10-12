package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.Meeting;
import com.mahesh.mentee_connect.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/join")
@Tag(name = "Join", description = "Meeting join page APIs")
public class JoinController {

    @Autowired
    private MeetingService meetingService;

    @GetMapping
    @Operation(summary = "Get meeting join info", description = "Get meeting information for join page")
    public ResponseEntity<Map<String, Object>> getMeetingJoinInfo(@RequestParam String roomId) {
        try {
            Meeting meeting = meetingService.getMeetingByRoomId(roomId);
            
            return ResponseEntity.ok(Map.of(
                "roomId", meeting.getRoomId(),
                "title", meeting.getTitle(),
                "description", meeting.getDescription() != null ? meeting.getDescription() : "",
                "scheduledTime", meeting.getScheduledTime().toString(),
                "passwordRequired", meeting.isPasswordRequired(),
                "mentorName", meeting.getMentor().getUsername(),
                "status", meeting.getStatus().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Meeting not found or invalid room ID",
                "message", e.getMessage()
            ));
        }
    }
}