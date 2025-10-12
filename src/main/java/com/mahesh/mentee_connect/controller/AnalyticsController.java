package com.mahesh.mentee_connect.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.mahesh.mentee_connect.model.MentorEngagementStats;
import com.mahesh.mentee_connect.model.StudentProgressStats;
import com.mahesh.mentee_connect.model.MeetingMetrics;
import com.mahesh.mentee_connect.service.AnalyticsService;

@RestController
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/mentor-engagement")
    public ResponseEntity<MentorEngagementStats> getMentorEngagementStats() {
        return ResponseEntity.ok(analyticsService.getMentorEngagementStats());
    }

    @GetMapping("/student-progress")
    public ResponseEntity<List<StudentProgressStats>> getStudentProgressStats() {
        return ResponseEntity.ok(analyticsService.getStudentProgressStats());
    }

    @GetMapping("/meeting-metrics")
    public ResponseEntity<MeetingMetrics> getMeetingMetrics(
            @RequestParam(required = false) String timeRange) {
        return ResponseEntity.ok(analyticsService.getMeetingMetrics(timeRange));
    }
}
