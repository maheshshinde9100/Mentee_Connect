package com.mahesh.mentee_connect.service;

import java.time.LocalDateTime;
import java.util.List;
import com.mahesh.mentee_connect.model.Meeting;
import com.mahesh.mentee_connect.model.MeetingParticipant;
import com.mahesh.mentee_connect.payload.request.MeetingRequest;
import com.mahesh.mentee_connect.dto.MeetingChangeRequest;
import com.mahesh.mentee_connect.dto.MeetingCreateResponse;

public interface MeetingService {
    Meeting createMeeting(Meeting meeting);
    MeetingCreateResponse createEnhancedMeeting(Meeting meeting);
    Meeting updateMeeting(String id, Meeting meeting);
    void deleteMeeting(String id);
    Meeting getMeetingById(String id);
    Meeting getMeetingByRoomId(String roomId);
    List<Meeting> getAllMeetings();
    List<Meeting> getMeetingsByMentor(String mentorId);
    List<Meeting> getMeetingsByStudent(String studentId);
    List<Meeting> getMeetingsByMentorAndStudent(String mentorId, String studentId);
    List<Meeting> getMeetingsByTimeRange(LocalDateTime start, LocalDateTime end);
    List<Meeting> getMeetingsByMentorAndTimeRange(String mentorId, LocalDateTime start, LocalDateTime end);
    List<Meeting> getMeetingsByStudentAndTimeRange(String studentId, LocalDateTime start, LocalDateTime end);
    Meeting updateMeetingStatus(String id, Meeting.MeetingStatus status);
    Meeting addMeetingNotes(String id, String notes);
    Meeting scheduleMeeting(MeetingRequest request);
    List<Meeting> getUpcomingMeetingsByMentor(String mentorId);
    List<Meeting> getUpcomingMeetingsByStudent(String studentId);
    Meeting getMeetingDetailsForStudent(String meetingId, String studentId);
    MeetingChangeRequest requestMeetingChanges(String meetingId, String studentId, MeetingChangeRequest request);
    
    // New methods for enhanced meeting system
    MeetingParticipant requestToJoinMeeting(String roomId, String participantId, String password);
    MeetingParticipant approveParticipant(String roomId, String participantId, String approverId);
    MeetingParticipant rejectParticipant(String roomId, String participantId, String approverId, String reason);
    List<MeetingParticipant> getWaitingParticipants(String roomId);
    List<MeetingParticipant> getAllParticipants(String roomId);
}
