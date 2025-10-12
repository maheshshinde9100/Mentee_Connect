package com.mahesh.mentee_connect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class VideoCallPathConfig implements WebMvcConfigurer {
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // This will be handled by the existing VideoCallController
        // The /api prefix is already handled by server.servlet.context-path
    }
}