package com.mahesh.mentee_connect.service;

import com.mahesh.mentee_connect.payload.response.AnalyticsResponse;
import com.mahesh.mentee_connect.model.MentorEngagementStats;
import com.mahesh.mentee_connect.model.StudentProgressStats;
import java.util.List;

public interface AnalyticsService {
    MentorEngagementStats getMentorEngagementStats();
    List<StudentProgressStats> getStudentProgressStats();
    AnalyticsResponse.UserCountStats getUserCountStats();
    AnalyticsResponse.DepartmentStats getDepartmentStats();
    AnalyticsResponse.StudentStats getStudentStats();
    AnalyticsResponse.MentorStats getMentorStats();
    AnalyticsResponse getAllAnalytics();
}