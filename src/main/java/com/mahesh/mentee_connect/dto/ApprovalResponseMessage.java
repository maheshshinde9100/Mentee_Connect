package com.mahesh.mentee_connect.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponseMessage {
    private String status; // "approved" or "rejected"
    private String roomId;
    private String meetingTitle;
    private String message;
    private String joinLink;
}