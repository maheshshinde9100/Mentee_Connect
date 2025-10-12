package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.Meeting;
import com.mahesh.mentee_connect.model.MeetingParticipant;
import com.mahesh.mentee_connect.service.MeetingService;
import com.mahesh.mentee_connect.dto.MeetingCreateResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping({"/meetings", "/api/meetings"})
@Tag(name = "Meetings", description = "Meeting management APIs")
public class MeetingController {
    @Autowired
    private MeetingService meetingService;

    @PostMapping
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Create meeting", description = "Create a new meeting (Mentor only)")
    public ResponseEntity<Meeting> createMeeting(@Valid @RequestBody Meeting meeting) {
        return ResponseEntity.ok(meetingService.createMeeting(meeting));
    }

    @PostMapping("/enhanced")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Create enhanced meeting", description = "Create a new meeting with room ID and join link (Mentor only)")
    public ResponseEntity<MeetingCreateResponse> createEnhancedMeeting(@Valid @RequestBody Meeting meeting) {
        return ResponseEntity.ok(meetingService.createEnhancedMeeting(meeting));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Update meeting", description = "Update an existing meeting (Mentor only)")
    public ResponseEntity<Meeting> updateMeeting(@PathVariable String id, @Valid @RequestBody Meeting meeting) {
        return ResponseEntity.ok(meetingService.updateMeeting(id, meeting));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Delete meeting", description = "Delete a meeting (Mentor only)")
    public ResponseEntity<?> deleteMeeting(@PathVariable String id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR') or hasRole('STUDENT')")
    @Operation(summary = "Get meeting", description = "Get meeting by ID")
    public ResponseEntity<Meeting> getMeeting(@PathVariable String id) {
        return ResponseEntity.ok(meetingService.getMeetingById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all meetings", description = "Get all meetings (Admin only)")
    public ResponseEntity<List<Meeting>> getAllMeetings() {
        return ResponseEntity.ok(meetingService.getAllMeetings());
    }

    @GetMapping("/mentor/{mentorId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#mentorId)")
    @Operation(summary = "Get mentor meetings", description = "Get all meetings for a mentor")
    public ResponseEntity<List<Meeting>> getMeetingsByMentor(@PathVariable String mentorId) {
        return ResponseEntity.ok(meetingService.getMeetingsByMentor(mentorId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR') or @securityService.isCurrentUser(#studentId)")
    @Operation(summary = "Get student meetings", description = "Get all meetings for a student")
    public ResponseEntity<List<Meeting>> getMeetingsByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(meetingService.getMeetingsByStudent(studentId));
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR') or hasRole('STUDENT')")
    @Operation(summary = "Get meetings by time range", description = "Get meetings within a time range")
    public ResponseEntity<List<Meeting>> getMeetingsByTimeRange(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(meetingService.getMeetingsByTimeRange(start, end));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Update meeting status", description = "Update the status of a meeting (Mentor only)")
    public ResponseEntity<Meeting> updateMeetingStatus(
            @PathVariable String id,
            @RequestParam Meeting.MeetingStatus status) {
        return ResponseEntity.ok(meetingService.updateMeetingStatus(id, status));
    }

    @PutMapping("/{id}/notes")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Add meeting notes", description = "Add notes to a meeting (Mentor only)")
    public ResponseEntity<Meeting> addMeetingNotes(
            @PathVariable String id,
            @RequestParam String notes) {
        return ResponseEntity.ok(meetingService.addMeetingNotes(id, notes));
    }

    // New enhanced meeting endpoints
    @PostMapping("/{roomId}/join")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Request to join meeting", description = "Student requests to join a meeting room")
    public ResponseEntity<MeetingParticipant> requestToJoinMeeting(
            @PathVariable String roomId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        String password = request != null ? request.get("password") : null;
        String participantId = authentication.getName(); // Get user ID from authentication
        return ResponseEntity.ok(meetingService.requestToJoinMeeting(roomId, participantId, password));
    }

    @PostMapping("/{roomId}/approve/{participantId}")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Approve participant", description = "Mentor approves a participant to join the meeting")
    public ResponseEntity<MeetingParticipant> approveParticipant(
            @PathVariable String roomId,
            @PathVariable String participantId,
            Authentication authentication) {
        String approverId = authentication.getName();
        return ResponseEntity.ok(meetingService.approveParticipant(roomId, participantId, approverId));
    }

    @PostMapping("/{roomId}/reject/{participantId}")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Reject participant", description = "Mentor rejects a participant from joining the meeting")
    public ResponseEntity<MeetingParticipant> rejectParticipant(
            @PathVariable String roomId,
            @PathVariable String participantId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        String approverId = authentication.getName();
        String reason = request != null ? request.get("reason") : null;
        return ResponseEntity.ok(meetingService.rejectParticipant(roomId, participantId, approverId, reason));
    }

    @GetMapping("/{roomId}/waiting")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Get waiting participants", description = "Get list of participants waiting for approval")
    public ResponseEntity<List<MeetingParticipant>> getWaitingParticipants(@PathVariable String roomId) {
        return ResponseEntity.ok(meetingService.getWaitingParticipants(roomId));
    }

    @GetMapping("/{roomId}/participants")
    @PreAuthorize("hasRole('MENTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get all participants", description = "Get list of all participants for a meeting room")
    public ResponseEntity<List<MeetingParticipant>> getAllParticipants(@PathVariable String roomId) {
        return ResponseEntity.ok(meetingService.getAllParticipants(roomId));
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasRole('MENTOR') or hasRole('STUDENT')")
    @Operation(summary = "Get meeting by room ID", description = "Get meeting details by room ID")
    public ResponseEntity<Meeting> getMeetingByRoomId(@PathVariable String roomId) {
        return ResponseEntity.ok(meetingService.getMeetingByRoomId(roomId));
    }
}