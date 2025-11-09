package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.payload.response.AnalyticsResponse;
import com.mahesh.mentee_connect.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "https://menteeconnect.vercel.app"})
public class AdminAnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsResponse> getAllAnalytics() {
        logger.info("Fetching all analytics data");
        return ResponseEntity.ok(analyticsService.getAllAnalytics());
    }

    @GetMapping("/users")
    public ResponseEntity<AnalyticsResponse.UserCountStats> getUserStats() {
        logger.info("Fetching user statistics");
        return ResponseEntity.ok(analyticsService.getUserCountStats());
    }

    @GetMapping("/departments")
    public ResponseEntity<AnalyticsResponse.DepartmentStats> getDepartmentStats() {
        logger.info("Fetching department statistics");
        return ResponseEntity.ok(analyticsService.getDepartmentStats());
    }

    @GetMapping("/students")
    public ResponseEntity<AnalyticsResponse.StudentStats> getStudentStats() {
        logger.info("Fetching student statistics");
        return ResponseEntity.ok(analyticsService.getStudentStats());
    }

    @GetMapping("/mentors")
    public ResponseEntity<AnalyticsResponse.MentorStats> getMentorStats() {
        logger.info("Fetching mentor statistics");
        return ResponseEntity.ok(analyticsService.getMentorStats());
    }
} 