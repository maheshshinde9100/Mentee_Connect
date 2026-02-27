package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.dto.VideoCallDtos;
import com.mahesh.mentee_connect.dto.VideoCallDtos.AdminVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.MenteeInfo;
import com.mahesh.mentee_connect.dto.VideoCallDtos.MentorVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.VideoCallStartRequest;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
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
        Mentor mentor = mentorRepository.findById(request.getMentorId())
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", request.getMentorId()));

        List<Student> mentees = new ArrayList<>();
        if (request.getMenteeIds() != null && !request.getMenteeIds().isEmpty()) {
            mentees = studentRepository.findAllById(request.getMenteeIds());
        }

        LocalDateTime now = LocalDateTime.now();

        VideoCallRecord record = new VideoCallRecord();
        record.setMentorId(mentor.getId());
        record.setMentorName(mentor.getFirstName() + " " + mentor.getLastName());
        record.setMenteeIds(mentees.stream().map(Student::getId).collect(Collectors.toList()));
        record.setMenteeNames(mentees.stream()
                .map(s -> (s.getFirstName() != null ? s.getFirstName() : "") +
                        (s.getLastName() != null ? " " + s.getLastName() : ""))
                .collect(Collectors.toList()));
        record.setTitle(request.getTitle() != null ? request.getTitle() : "Mentor Call");
        record.setStartTime(now);
        record.setEndTime(null);

        return videoCallRecordRepository.save(record);
    }

    @Override
    public VideoCallRecord endCall(String mentorId, String callId) {
        VideoCallRecord record = videoCallRecordRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoCallRecord", "id", callId));

        if (!record.getMentorId().equals(mentorId)) {
            throw new ResourceNotFoundException("VideoCallRecord", "mentorId", mentorId);
        }

        if (record.getEndTime() == null) {
            record.setEndTime(LocalDateTime.now());
            record = videoCallRecordRepository.save(record);
        }

        return record;
    }

    @Override
    public List<AdminVideoCallDTO> getAllForAdmin() {
        return videoCallRecordRepository.findAllByOrderByStartTimeDesc().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MentorVideoCallDTO> getForMentor(String mentorId) {
        return videoCallRecordRepository.findByMentorIdOrderByStartTimeDesc(mentorId).stream()
                .map(this::toMentorDto)
                .collect(Collectors.toList());
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
                            .name((s.getFirstName() != null ? s.getFirstName() : "") +
                                    (s.getLastName() != null ? " " + s.getLastName() : ""))
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

