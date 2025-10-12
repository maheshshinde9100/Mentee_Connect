// 4. Video Call Controller (src/main/java/com/mahesh/mentee_connect/controller/VideoCallController.java)
package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.entity.VideoCall;
import com.mahesh.mentee_connect.service.VideoCallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/video-calls")
@CrossOrigin(origins = "*") // Configure as per your frontend URL
@PreAuthorize("hasRole('MENTOR') or hasRole('STUDENT')")
public class VideoCallController {

    @Autowired
    private VideoCallService videoCallService;

    @PostMapping
    public ResponseEntity<?> initializeVideoCall(@RequestBody Map<String, String> request) {
        try {
            String meetingId = request.get("meetingId");
            String mentorId = request.get("mentorId");
            
            VideoCall videoCall = videoCallService.initializeCall(meetingId, mentorId);
            return ResponseEntity.ok(videoCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error initializing video call: " + e.getMessage());
        }
    }

    // Create new video call (Mentor only)
    @PostMapping("/create")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<?> createVideoCall(@RequestBody Map<String, Object> request) {
        try {
            // Get the current authenticated mentor
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String mentorId = authentication.getName();
            
            String mentorName = (String) request.get("mentorName");
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            String scheduledTimeStr = (String) request.get("scheduledTime");
            Integer maxParticipants = (Integer) request.get("maxParticipants");
            String password = (String) request.get("password");
            
            LocalDateTime scheduledTime = LocalDateTime.parse(scheduledTimeStr);
            
            VideoCall videoCall = videoCallService.createVideoCall(
                mentorId, mentorName, title, description, scheduledTime, maxParticipants, password
            );
            
            return ResponseEntity.ok(videoCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating video call: " + e.getMessage());
        }
    }

    // Get video calls by mentor
    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<List<VideoCall>> getVideoCallsByMentor(@PathVariable String mentorId) {
        List<VideoCall> videoCalls = videoCallService.getVideoCallsByMentor(mentorId);
        return ResponseEntity.ok(videoCalls);
    }

    // Get available video calls for mentees
    @GetMapping("/available")
    public ResponseEntity<List<VideoCall>> getAvailableVideoCalls() {
        List<VideoCall> videoCalls = videoCallService.getAvailableVideoCallsForMentees();
        return ResponseEntity.ok(videoCalls);
    }

    // Join video call
    @PostMapping("/join")
    public ResponseEntity<?> joinVideoCall(@RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            String participantName = request.get("participantName");
            String participantId = request.get("participantId");
            String password = request.get("password");
            
            // If participantId is not provided, get from authentication
            if (participantId == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                participantId = authentication.getName();
            }
            
            VideoCall videoCall = videoCallService.joinVideoCall(roomId, participantId, participantName, password);
            
            // Return the expected response format
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Join request sent successfully",
                "status", "waiting_for_approval"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Error joining video call: " + e.getMessage(),
                "status", "error"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Start video call (Mentor only)
    @PostMapping("/start")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<?> startVideoCall(@RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            
            // Get the current authenticated mentor
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String mentorId = authentication.getName();
            
            VideoCall videoCall = videoCallService.startVideoCall(roomId, mentorId);
            return ResponseEntity.ok(videoCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error starting video call: " + e.getMessage());
        }
    }

    // End video call (Mentor only)
    @PostMapping("/end")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<?> endVideoCall(@RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            
            // Get the current authenticated mentor
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String mentorId = authentication.getName();
            
            VideoCall videoCall = videoCallService.endVideoCall(roomId, mentorId);
            return ResponseEntity.ok(videoCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error ending video call: " + e.getMessage());
        }
    }

    // Leave video call
    @PostMapping("/leave")
    public ResponseEntity<?> leaveVideoCall(@RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            
            // Get the current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String participantId = authentication.getName();
            
            VideoCall videoCall = videoCallService.leaveVideoCall(roomId, participantId);
            return ResponseEntity.ok(videoCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error leaving video call: " + e.getMessage());
        }
    }

    // Get video call details by room ID
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getVideoCallByRoomId(@PathVariable String roomId) {
        try {
            VideoCall videoCall = videoCallService.getVideoCallByRoomId(roomId);
            return ResponseEntity.ok(videoCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching video call: " + e.getMessage());
        }
    }

    // Get video calls for mentee
    @GetMapping("/mentee/{menteeId}")
    public ResponseEntity<List<VideoCall>> getVideoCallsForMentee(@PathVariable String menteeId) {
        List<VideoCall> videoCalls = videoCallService.getVideoCallsForMentee(menteeId);
        return ResponseEntity.ok(videoCalls);
    }

    // Approve participant to join (Mentor only)
    @PostMapping("/approve")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<?> approveParticipant(@RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            String participantId = request.get("participantId");
            
            // Get the current authenticated mentor
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String mentorId = authentication.getName();
            
            VideoCall videoCall = videoCallService.approveParticipant(roomId, mentorId, participantId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Participant approved successfully",
                "videoCall", videoCall
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Error approving participant: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Reject participant (Mentor only)
    @PostMapping("/reject")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<?> rejectParticipant(@RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            String participantId = request.get("participantId");
            
            // Get the current authenticated mentor
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String mentorId = authentication.getName();
            
            VideoCall videoCall = videoCallService.rejectParticipant(roomId, mentorId, participantId);
            return ResponseEntity.ok(videoCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error rejecting participant: " + e.getMessage());
        }
    }

    // Get waiting participants (Mentor only)
    @GetMapping("/waiting/{roomId}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<?> getWaitingParticipants(@PathVariable String roomId) {
        try {
            // Get the current authenticated mentor
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String mentorId = authentication.getName();
            
            List<String> waitingParticipants = videoCallService.getWaitingParticipants(roomId, mentorId);
            return ResponseEntity.ok(waitingParticipants);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching waiting participants: " + e.getMessage());
        }
    }

    // Generate shareable meeting link
    @GetMapping("/link/{roomId}")
    public ResponseEntity<?> getMeetingLink(@PathVariable String roomId) {
        try {
            VideoCall videoCall = videoCallService.getVideoCallByRoomId(roomId);
            return ResponseEntity.ok(Map.of("joinLink", videoCall.getJoinLink(), "roomId", roomId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating meeting link: " + e.getMessage());
        }
    }

    // Test WebSocket notification endpoint
    @PostMapping("/test-websocket")
    public ResponseEntity<?> testWebSocketNotification(@RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            String participantId = request.get("participantId");
            String participantName = request.get("participantName");
            
            videoCallService.sendTestJoinRequest(roomId, participantId, participantName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test WebSocket notification sent"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error sending test notification: " + e.getMessage()
            ));
        }
    }
}