package com.mahesh.mentee_connect.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingCreateResponse {
    private String meetingId;
    private String roomId;
    private String title;
    private String joinLink;
    private boolean passwordRequired;
    private String password;
    private String description;
    private String scheduledTime;
}