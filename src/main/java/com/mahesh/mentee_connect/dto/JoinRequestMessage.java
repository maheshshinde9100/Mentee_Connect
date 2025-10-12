package com.mahesh.mentee_connect.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinRequestMessage {
    private String action;
    private String participantId;
    private String participantName;
    private String roomId;
    private String meetingTitle;
    private LocalDateTime timestamp;
}