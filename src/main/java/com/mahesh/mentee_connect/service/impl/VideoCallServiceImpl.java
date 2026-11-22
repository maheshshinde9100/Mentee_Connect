package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.dto.VideoCallDtos.AdminVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.MenteeInfo;
import com.mahesh.mentee_connect.dto.VideoCallDtos.MentorVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.VideoCallStartRequest;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import com.mahesh.mentee_connect.exception.UnauthorizedException;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.VideoCallRecord;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.repository.VideoCallRecordRepository;
import com.mahesh.mentee_connect.service.VideoCallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoCallServiceImpl implements VideoCallService {

    @Autowired
    private VideoCallRecordRepository videoCallRecordRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public VideoCallRecord startCall(VideoCallStartRequest request) {
        if (request.getMentorId() == null || request.getMentorId().isBlank()) {
            throw new IllegalArgumentException("mentorId is required");
        }
        Mentor mentor = mentorRepository.findById(request.getMentorId())
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", request.getMentorId()));
        List<Student> mentees = findStudents(request.getMenteeIds());
        return persistRecord(mentor, mentees, request.getTitle());
    }

    @Override
    public VideoCallRecord startCallForUser(String userId, VideoCallStartRequest request) {
        Mentor mentor = mentorRepository.findById(userId).orElse(null);
        if (mentor != null) {
            List<Student> mentees = findStudents(request.getMenteeIds());
            return persistRecord(mentor, mentees, request.getTitle());
        }

        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Only mentors and students can start calls"));

        String mentorId = request.getMentorId();
        if ((mentorId == null || mentorId.isBlank()) && student.getAssignedMentor() != null) {
            mentorId = student.getAssignedMentor().getId();
        }
        if (mentorId == null || mentorId.isBlank()) {
            throw new IllegalArgumentException("No mentor is assigned to this student");
        }

        final String resolvedMentorId = mentorId;
        Mentor assignedMentor = mentorRepository.findById(resolvedMentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", resolvedMentorId));

        return persistRecord(assignedMentor, List.of(student),
                request.getTitle() != null ? request.getTitle() : "Student Call");
    }

    @Override
    public VideoCallRecord endCall(String mentorId, String callId) {
        VideoCallRecord record = videoCallRecordRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoCallRecord", "id", callId));

        if (!record.getMentorId().equals(mentorId)) {
            throw new UnauthorizedException("This call does not belong to the given mentor");
        }
        return markEnded(record);
    }

    @Override
    public VideoCallRecord endCallByParticipant(String userId, String callId) {
        VideoCallRecord record = videoCallRecordRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoCallRecord", "id", callId));

        boolean isMentor = userId.equals(record.getMentorId());
        boolean isMentee = record.getMenteeIds() != null && record.getMenteeIds().contains(userId);
        if (!isMentor && !isMentee) {
            throw new UnauthorizedException("You are not a participant in this call");
        }
        return markEnded(record);
    }

    @Override
    public List<AdminVideoCallDTO> getAllForAdmin() {
        return videoCallRecordRepository.findAllByOrderByStartTimeDesc().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminVideoCallDTO> getForUser(String userId) {
        if (mentorRepository.existsById(userId)) {
            return videoCallRecordRepository.findByMentorIdOrderByStartTimeDesc(userId).stream()
                    .map(this::toAdminDto)
                    .collect(Collectors.toList());
        }
        if (studentRepository.existsById(userId)) {
            return videoCallRecordRepository.findByMenteeIdsContainingOrderByStartTimeDesc(userId).stream()
                    .map(this::toAdminDto)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<MentorVideoCallDTO> getForMentor(String mentorId) {
        return videoCallRecordRepository.findByMentorIdOrderByStartTimeDesc(mentorId).stream()
                .map(this::toMentorDto)
                .collect(Collectors.toList());
    }

    private List<Student> findStudents(List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Student> mentees = studentRepository.findAllById(studentIds);
        if (mentees.size() != studentIds.size()) {
            throw new ResourceNotFoundException("Student", "ids", studentIds.toString());
        }
        return new ArrayList<>(mentees);
    }

    private VideoCallRecord persistRecord(Mentor mentor, List<Student> mentees, String title) {
        VideoCallRecord record = new VideoCallRecord();
        record.setMentorId(mentor.getId());
        record.setMentorName(displayName(mentor.getFirstName(), mentor.getLastName()));
        record.setMenteeIds(mentees.stream().map(Student::getId).collect(Collectors.toList()));
        record.setMenteeNames(mentees.stream()
                .map(s -> displayName(s.getFirstName(), s.getLastName()))
                .collect(Collectors.toList()));
        record.setTitle(title != null && !title.isBlank() ? title : "Video Call");
        record.setStartTime(LocalDateTime.now());
        record.setEndTime(null);
        return videoCallRecordRepository.save(record);
    }

    private VideoCallRecord markEnded(VideoCallRecord record) {
        if (record.getEndTime() == null) {
            record.setEndTime(LocalDateTime.now());
            return videoCallRecordRepository.save(record);
        }
        return record;
    }

    private String displayName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    private AdminVideoCallDTO toAdminDto(VideoCallRecord record) {
        LocalDateTime start = record.getStartTime();
        LocalDateTime end = record.getEndTime();
        LocalDate date = start != null ? start.toLocalDate() : null;
        LocalTime startTime = start != null ? start.toLocalTime() : null;
        LocalTime endTime = end != null ? end.toLocalTime() : null;

        int menteeCount = record.getMenteeIds() != null ? record.getMenteeIds().size() : 0;

        return AdminVideoCallDTO.builder()
                .id(record.getId())
                .mentorId(record.getMentorId())
                .mentorName(record.getMentorName())
                .meetingDate(date)
                .startTime(startTime)
                .endTime(endTime)
                .menteeCount(menteeCount)
                .build();
    }

    private MentorVideoCallDTO toMentorDto(VideoCallRecord record) {
        LocalDateTime start = record.getStartTime();
        LocalDateTime end = record.getEndTime();
        LocalDate date = start != null ? start.toLocalDate() : null;
        LocalTime startTime = start != null ? start.toLocalTime() : null;
        LocalTime endTime = end != null ? end.toLocalTime() : null;

        List<MenteeInfo> mentees = new ArrayList<>();
        if (record.getMenteeIds() != null && !record.getMenteeIds().isEmpty()) {
            List<Student> students = studentRepository.findAllById(record.getMenteeIds());
            mentees = students.stream()
                    .map(s -> MenteeInfo.builder()
                            .id(s.getId())
                            .name(displayName(s.getFirstName(), s.getLastName()))
                            .email(s.getEmail())
                            .build())
                    .collect(Collectors.toList());
        }

        return MentorVideoCallDTO.builder()
                .id(record.getId())
                .title(record.getTitle())
                .meetingDate(date)
                .startTime(startTime)
                .endTime(endTime)
                .mentees(mentees)
                .build();
    }
}
