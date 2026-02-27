package com.mahesh.mentee_connect.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "video_call_records")
public class VideoCallRecord {

    @Id
    private String id;

    private String mentorId;
    private String mentorName;

    private List<String> menteeIds;
    private List<String> menteeNames;

    private String title;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

