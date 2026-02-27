package com.mahesh.mentee_connect.repository;

import com.mahesh.mentee_connect.model.VideoCallRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VideoCallRecordRepository extends MongoRepository<VideoCallRecord, String> {

    List<VideoCallRecord> findByMentorIdOrderByStartTimeDesc(String mentorId);

    List<VideoCallRecord> findAllByOrderByStartTimeDesc();
}

