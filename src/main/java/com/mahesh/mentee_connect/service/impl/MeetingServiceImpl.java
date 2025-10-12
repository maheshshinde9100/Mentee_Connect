package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.Meeting;
import com.mahesh.mentee_connect.model.MeetingParticipant;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.repository.MeetingRepository;
import com.mahesh.mentee_connect.repository.MeetingParticipantRepository;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.service.MeetingService;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import com.mahesh.mentee_connect.payload.request.MeetingRequest;
import com.mahesh.mentee_connect.dto.MeetingChangeRequest;
import com.mahesh.mentee_connect.dto.MeetingCreateResponse;
import com.mahesh.mentee_connect.dto.JoinRequestMessage;
import com.mahesh.mentee_connect.dto.ApprovalResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingServiceImpl implements MeetingService {
    
    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional
    public Meeting createMeeting(Meeting meeting) {
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setUpdatedAt(LocalDateTime.now());
        return meetingRepository.save(meeting);
    }

    @Override
    @Transactional
    public MeetingCreateResponse createEnhancedMeeting(Meeting meeting) {
        // Generate unique room ID
        String roomId = UUID.randomUUID().toString();
        meeting.setRoomId(roomId);
        
        // Generate join link
        String joinLink = baseUrl + "/api/join?roomId=" + roomId;
        meeting.setJoinLink(joinLink);
        
        // Set timestamps
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setUpdatedAt(LocalDateTime.now());
        
        Meeting savedMeeting = meetingRepository.save(meeting);
        
        return MeetingCreateResponse.builder()
                .meetingId(savedMeeting.getId())
                .roomId(roomId)
                .title(savedMeeting.getTitle())
                .joinLink(joinLink)
                .passwordRequired(savedMeeting.isPasswordRequired())
                .password(savedMeeting.getPassword())
                .description(savedMeeting.getDescription())
                .scheduledTime(savedMeeting.getScheduledTime().toString())
                .build();
    }

    @Override
    @Transactional
    public Meeting updateMeeting(String id, Meeting meetingDetails) {
        Meeting meeting = getMeetingById(id);
        meeting.setTitle(meetingDetails.getTitle());
        meeting.setDescription(meetingDetails.getDescription());
        meeting.setScheduledTime(meetingDetails.getScheduledTime());
        meeting.setEndTime(meetingDetails.getEndTime());
        meeting.setStatus(meetingDetails.getStatus());
        meeting.setMeetingLink(meetingDetails.getMeetingLink());
        meeting.setLocation(meetingDetails.getLocation());
        meeting.setType(meetingDetails.getType());
        meeting.setNotes(meetingDetails.getNotes());
        meeting.setUpdatedAt(LocalDateTime.now());
        return meetingRepository.save(meeting);
    }

    @Override
    @Transactional
    public void deleteMeeting(String id) {
        Meeting meeting = getMeetingById(id);
        meetingRepository.delete(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public Meeting getMeetingById(String id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Meeting getMeetingByRoomId(String roomId) {
        return meetingRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with room id: " + roomId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByMentor(String mentorId) {
        return meetingRepository.findByMentorId(mentorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByStudent(String studentId) {
        return meetingRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByMentorAndStudent(String mentorId, String studentId) {
        return meetingRepository.findByMentorIdAndStudentId(mentorId, studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return meetingRepository.findByScheduledTimeBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByMentorAndTimeRange(String mentorId, LocalDateTime start, LocalDateTime end) {
        return meetingRepository.findByMentorIdAndScheduledTimeBetween(mentorId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMeetingsByStudentAndTimeRange(String studentId, LocalDateTime start, LocalDateTime end) {
        return meetingRepository.findByStudentIdAndScheduledTimeBetween(studentId, start, end);
    }

    @Override
    @Transactional
    public Meeting updateMeetingStatus(String id, Meeting.MeetingStatus status) {
        Meeting meeting = getMeetingById(id);
        meeting.setStatus(status);
        meeting.setUpdatedAt(LocalDateTime.now());
        return meetingRepository.save(meeting);
    }

    @Override
    @Transactional
    public Meeting addMeetingNotes(String id, String notes) {
        Meeting meeting = getMeetingById(id);
        meeting.setNotes(notes);
        meeting.setUpdatedAt(LocalDateTime.now());
        return meetingRepository.save(meeting);
    }

    @Override
    @Transactional
    public Meeting scheduleMeeting(MeetingRequest request) {
        Mentor mentor = mentorRepository.findById(request.getMentorId())
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", request.getMentorId()));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));

        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setScheduledTime(request.getMeetingTime());
        meeting.setMentor(mentor);
        meeting.setStudent(student);
        meeting.setStatus(Meeting.MeetingStatus.SCHEDULED);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setUpdatedAt(LocalDateTime.now());
        
        return meetingRepository.save(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getUpcomingMeetingsByMentor(String mentorId) {
        return meetingRepository.findByMentorIdAndStatusAndScheduledTimeAfter(
            mentorId, Meeting.MeetingStatus.SCHEDULED, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getUpcomingMeetingsByStudent(String studentId) {
        return meetingRepository.findByStudentIdAndStatusAndScheduledTimeAfter(
            studentId, Meeting.MeetingStatus.SCHEDULED, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Meeting getMeetingDetailsForStudent(String meetingId, String studentId) {
        Meeting meeting = getMeetingById(meetingId);
        if (!meeting.getStudent().getId().equals(studentId)) {
            throw new ResourceNotFoundException("Meeting not found for student", "id", meetingId);
        }
        return meeting;
    }

    @Override
    @Transactional
    public MeetingChangeRequest requestMeetingChanges(String meetingId, String studentId, MeetingChangeRequest request) {
        Meeting meeting = getMeetingDetailsForStudent(meetingId, studentId);
        // Store the change request and return it
        // You may want to add additional validation/processing here
        return request;
    }

    @Override
    @Transactional
    public MeetingParticipant requestToJoinMeeting(String roomId, String participantId, String password) {
        Meeting meeting = getMeetingByRoomId(roomId);
        
        // Validate password if required
        if (meeting.isPasswordRequired() && !meeting.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        // Check if participant already exists
        var existingParticipant = participantRepository.findByRoomIdAndParticipantId(roomId, participantId);
        if (existingParticipant.isPresent()) {
            return existingParticipant.get();
        }
        
        // Create new participant request
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(meeting.getId());
        participant.setRoomId(roomId);
        
        // Set participant user (you might need to fetch the user)
        User user = new User();
        user.setId(participantId);
        participant.setParticipant(user);
        
        participant.setStatus(MeetingParticipant.ParticipantStatus.WAITING);
        participant.setRequestedAt(LocalDateTime.now());
        
        MeetingParticipant savedParticipant = participantRepository.save(participant);
        
        // Send WebSocket notification to mentor
        JoinRequestMessage joinRequest = JoinRequestMessage.builder()
                .action("join_request")
                .participantId(participantId)
                .participantName("Student") // You might want to fetch actual name
                .roomId(roomId)
                .meetingTitle(meeting.getTitle())
                .timestamp(LocalDateTime.now())
                .build();
        
        messagingTemplate.convertAndSend("/topic/meeting/" + roomId + "/join-requests", joinRequest);
        
        return savedParticipant;
    }

    @Override
    @Transactional
    public MeetingParticipant approveParticipant(String roomId, String participantId, String approverId) {
        Meeting meeting = getMeetingByRoomId(roomId);
        
        // Verify approver is the mentor
        if (!meeting.getMentor().getId().equals(approverId)) {
            throw new IllegalArgumentException("Only meeting host can approve participants");
        }
        
        MeetingParticipant participant = participantRepository.findByRoomIdAndParticipantId(roomId, participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        
        participant.setStatus(MeetingParticipant.ParticipantStatus.APPROVED);
        participant.setRespondedAt(LocalDateTime.now());
        participant.setResponseMessage("Approved to join the meeting");
        
        MeetingParticipant savedParticipant = participantRepository.save(participant);
        
        // Send WebSocket notification to participant
        ApprovalResponseMessage response = ApprovalResponseMessage.builder()
                .status("approved")
                .roomId(roomId)
                .meetingTitle(meeting.getTitle())
                .message("You have been approved to join the meeting")
                .joinLink(meeting.getJoinLink())
                .build();
        
        messagingTemplate.convertAndSend("/queue/user/" + participantId + "/meeting-approval", response);
        
        return savedParticipant;
    }

    @Override
    @Transactional
    public MeetingParticipant rejectParticipant(String roomId, String participantId, String approverId, String reason) {
        Meeting meeting = getMeetingByRoomId(roomId);
        
        // Verify approver is the mentor
        if (!meeting.getMentor().getId().equals(approverId)) {
            throw new IllegalArgumentException("Only meeting host can reject participants");
        }
        
        MeetingParticipant participant = participantRepository.findByRoomIdAndParticipantId(roomId, participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        
        participant.setStatus(MeetingParticipant.ParticipantStatus.REJECTED);
        participant.setRespondedAt(LocalDateTime.now());
        participant.setResponseMessage(reason != null ? reason : "Request rejected");
        
        MeetingParticipant savedParticipant = participantRepository.save(participant);
        
        // Send WebSocket notification to participant
        ApprovalResponseMessage response = ApprovalResponseMessage.builder()
                .status("rejected")
                .roomId(roomId)
                .meetingTitle(meeting.getTitle())
                .message(reason != null ? reason : "Your request to join the meeting was rejected")
                .build();
        
        messagingTemplate.convertAndSend("/queue/user/" + participantId + "/meeting-approval", response);
        
        return savedParticipant;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingParticipant> getWaitingParticipants(String roomId) {
        return participantRepository.findByRoomIdAndStatus(roomId, MeetingParticipant.ParticipantStatus.WAITING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingParticipant> getAllParticipants(String roomId) {
        return participantRepository.findByRoomId(roomId);
    }
}