package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.dto.VideoCallDtos.AdminVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.MentorVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.VideoCallStartRequest;
import com.mahesh.mentee_connect.model.VideoCallRecord;
import com.mahesh.mentee_connect.security.services.SecurityService;
import com.mahesh.mentee_connect.service.VideoCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@CrossOrigin(origins = {"http://localhost:3000", "https://menteeconnect.vercel.app"})
@Tag(name = "Video Calls", description = "Video call history APIs")
public class VideoCallController {

    @Autowired
    private VideoCallService videoCallService;

    @Autowired
    private SecurityService securityService;

    @PostMapping("/mentors/{mentorId}/video-calls/start")
    @PreAuthorize("hasRole('MENTOR') and @securityService.isCurrentUser(#mentorId)")
    @Operation(summary = "Start a video call", description = "Create a video call record when mentor starts a call")
    public ResponseEntity<VideoCallRecord> startCall(
            @PathVariable String mentorId,
            @RequestBody VideoCallStartRequest request) {
        request.setMentorId(mentorId);
        VideoCallRecord created = videoCallService.startCall(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/mentors/{mentorId}/video-calls/{callId}/end")
    @PreAuthorize("hasRole('MENTOR') and @securityService.isCurrentUser(#mentorId)")
    @Operation(summary = "End a video call", description = "Mark a video call as ended")
    public ResponseEntity<VideoCallRecord> endCall(
            @PathVariable String mentorId,
            @PathVariable String callId) {
        VideoCallRecord updated = videoCallService.endCall(mentorId, callId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/admin/video-calls")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all video calls for admin", description = "List all video call records for admin view")
    public ResponseEntity<List<AdminVideoCallDTO>> getAllForAdmin() {
        return ResponseEntity.ok(videoCallService.getAllForAdmin());
    }

    @GetMapping("/mentors/{mentorId}/video-calls")
    @PreAuthorize("hasRole('MENTOR') and @securityService.isCurrentUser(#mentorId)")
    @Operation(summary = "Get mentor video calls", description = "List video call records for a specific mentor")
    public ResponseEntity<List<MentorVideoCallDTO>> getForMentor(@PathVariable String mentorId) {
        return ResponseEntity.ok(videoCallService.getForMentor(mentorId));
    }
}

