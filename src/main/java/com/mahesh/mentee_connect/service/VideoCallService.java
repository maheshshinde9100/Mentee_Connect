// 3. Video Call Service (src/main/java/com/mahesh/mentee_connect/service/VideoCallService.java)
package com.mahesh.mentee_connect.service;

import com.mahesh.mentee_connect.entity.VideoCall;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.repository.VideoCallRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.security.services.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VideoCallService {
    
    @Autowired
    private VideoCallRepository videoCallRepository;

    public VideoCall initializeCall(String meetingId, String mentorId) {
        VideoCall videoCall = new VideoCall();
        videoCall.setMentorId(mentorId);
        videoCall.setStatus("INITIALIZED");
        videoCall.setCreatedAt(LocalDateTime.now());
        videoCall.setRoomId(generateRoomId());
        return videoCallRepository.save(videoCall);
    }

    private String generateRoomId() {
        return "room_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Create a new video call (only mentors can create)
    public VideoCall createVideoCall(String mentorId, String mentorName, String title, 
                                   String description, LocalDateTime scheduledTime, 
                                   Integer maxParticipants, String password) {
        VideoCall videoCall = new VideoCall(mentorId, mentorName, title, description, scheduledTime);
        videoCall.setMaxParticipants(maxParticipants);
        videoCall.setMeetingPassword(password);
        return videoCallRepository.save(videoCall);
    }

    // Get all video calls by mentor
    public List<VideoCall> getVideoCallsByMentor(String mentorId) {
        return videoCallRepository.findByMentorId(mentorId);
    }

    // Get all available video calls (for mentees to join)
    public List<VideoCall> getAvailableVideoCallsForMentees() {
        return videoCallRepository.findByStatus("SCHEDULED");
    }

    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SecurityService securityService;
    
    // Join a video call (for mentees and mentors)
    public VideoCall joinVideoCall(String roomId, String participantId, String participantName, String password) {
        Optional<VideoCall> optionalCall = videoCallRepository.findByRoomId(roomId);
        
        if (optionalCall.isEmpty()) {
            throw new RuntimeException("Video call not found");
        }
        
        VideoCall videoCall = optionalCall.get();
        
        // Check password if required
        if (videoCall.getMeetingPassword() != null && !videoCall.getMeetingPassword().equals(password)) {
            throw new RuntimeException("Invalid meeting password");
        }
        
        // Check if already joined
        if (videoCall.getParticipants().contains(participantId)) {
            return videoCall; // Already joined
        }
        
        // Check max participants limit
        if (videoCall.getMaxParticipants() != null && 
            videoCall.getParticipants().size() >= videoCall.getMaxParticipants()) {
            throw new RuntimeException("Meeting is full");
        }
        
        // If participant is a student, check if they are assigned to the mentor who created the call
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        
        if (isStudent) {
            // Get the student by ID
            Student student = studentRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            // Check if the student is assigned to the mentor who created the call
            if (student.getAssignedMentor() == null || 
                !student.getAssignedMentor().getId().equals(videoCall.getMentorId())) {
                throw new RuntimeException("You can only join video calls hosted by your assigned mentor");
            }
        }
        
        // If meeting requires approval and participant is not the mentor, add to waiting room
        if (videoCall.isRequiresApproval() && !participantId.equals(videoCall.getMentorId())) {
            if (!videoCall.getWaitingParticipants().contains(participantId)) {
                videoCall.getWaitingParticipants().add(participantId);
                videoCall.setStatus("WAITING_FOR_HOST");
            }
        } else {
            // Add participant directly
            videoCall.getParticipants().add(participantId);
        }
        
        return videoCallRepository.save(videoCall);
    }

    // Start video call (only mentor can start)
    public VideoCall startVideoCall(String roomId, String mentorId) {
        Optional<VideoCall> optionalCall = videoCallRepository.findByRoomId(roomId);
        
        if (optionalCall.isEmpty()) {
            throw new RuntimeException("Video call not found");
        }
        
        VideoCall videoCall = optionalCall.get();
        
        if (!videoCall.getMentorId().equals(mentorId)) {
            throw new RuntimeException("Only the mentor can start the call");
        }
        
        videoCall.setStatus("LIVE");
        videoCall.setStartedAt(LocalDateTime.now());
        return videoCallRepository.save(videoCall);
    }

    // End video call (only mentor can end)
    public VideoCall endVideoCall(String roomId, String mentorId) {
        Optional<VideoCall> optionalCall = videoCallRepository.findByRoomId(roomId);
        
        if (optionalCall.isEmpty()) {
            throw new RuntimeException("Video call not found");
        }
        
        VideoCall videoCall = optionalCall.get();
        
        if (!videoCall.getMentorId().equals(mentorId)) {
            throw new RuntimeException("Only the mentor can end the call");
        }
        
        videoCall.setStatus("ENDED");
        videoCall.setEndedAt(LocalDateTime.now());
        return videoCallRepository.save(videoCall);
    }

    // Leave video call (for mentees)
    public VideoCall leaveVideoCall(String roomId, String menteeId) {
        Optional<VideoCall> optionalCall = videoCallRepository.findByRoomId(roomId);
        
        if (optionalCall.isEmpty()) {
            throw new RuntimeException("Video call not found");
        }
        
        VideoCall videoCall = optionalCall.get();
        videoCall.getParticipants().remove(menteeId);
        return videoCallRepository.save(videoCall);
    }

    // Get video call by room ID
    public VideoCall getVideoCallByRoomId(String roomId) {
        return videoCallRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Video call not found"));
    }

    // Get video calls that mentee has joined
    public List<VideoCall> getVideoCallsForMentee(String menteeId) {
        return videoCallRepository.findByParticipantsContaining(menteeId);
    }
    
    // Approve participant to join (mentor only)
    public VideoCall approveParticipant(String roomId, String mentorId, String participantId) {
        Optional<VideoCall> optionalCall = videoCallRepository.findByRoomId(roomId);
        
        if (optionalCall.isEmpty()) {
            throw new RuntimeException("Video call not found");
        }
        
        VideoCall videoCall = optionalCall.get();
        
        if (!videoCall.getMentorId().equals(mentorId)) {
            throw new RuntimeException("Only the mentor can approve participants");
        }
        
        if (videoCall.getWaitingParticipants().contains(participantId)) {
            videoCall.getWaitingParticipants().remove(participantId);
            videoCall.getParticipants().add(participantId);
        }
        
        return videoCallRepository.save(videoCall);
    }
    
    // Reject participant (mentor only)
    public VideoCall rejectParticipant(String roomId, String mentorId, String participantId) {
        Optional<VideoCall> optionalCall = videoCallRepository.findByRoomId(roomId);
        
        if (optionalCall.isEmpty()) {
            throw new RuntimeException("Video call not found");
        }
        
        VideoCall videoCall = optionalCall.get();
        
        if (!videoCall.getMentorId().equals(mentorId)) {
            throw new RuntimeException("Only the mentor can reject participants");
        }
        
        videoCall.getWaitingParticipants().remove(participantId);
        return videoCallRepository.save(videoCall);
    }
    
    // Get waiting participants (mentor only)
    public List<String> getWaitingParticipants(String roomId, String mentorId) {
        Optional<VideoCall> optionalCall = videoCallRepository.findByRoomId(roomId);
        
        if (optionalCall.isEmpty()) {
            throw new RuntimeException("Video call not found");
        }
        
        VideoCall videoCall = optionalCall.get();
        
        if (!videoCall.getMentorId().equals(mentorId)) {
            throw new RuntimeException("Only the mentor can view waiting participants");
        }
        
        return videoCall.getWaitingParticipants();
    }
}
