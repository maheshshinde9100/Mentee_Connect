package com.mahesh.mentee_connect.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class VideoCallDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VideoCallStartRequest {
        private String mentorId;
        private String title;
        private List<String> menteeIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminVideoCallDTO {
        private String id;
        private String mentorId;
        private String mentorName;
        private LocalDate meetingDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private int menteeCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MentorVideoCallDTO {
        private String id;
        private String title;
        private LocalDate meetingDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private List<MenteeInfo> mentees;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MenteeInfo {
        private String id;
        private String name;
        private String email;
    }
}

