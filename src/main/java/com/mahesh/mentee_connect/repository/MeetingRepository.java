// MeetingRepository.java
package com.mahesh.mentee_connect.repository;

import com.mahesh.mentee_connect.model.Meeting;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends MongoRepository<Meeting, String> {
    List<Meeting> findByMentorId(String mentorId);
    List<Meeting> findByStudentId(String studentId);
    List<Meeting> findByMentorIdAndStudentId(String mentorId, String studentId);
    List<Meeting> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Meeting> findByMentorIdAndScheduledTimeBetween(String mentorId, LocalDateTime start, LocalDateTime end);
    List<Meeting> findByStudentIdAndScheduledTimeBetween(String studentId, LocalDateTime start, LocalDateTime end);
    List<Meeting> findByMentorIdAndStatusAndScheduledTimeAfter(String mentorId, Meeting.MeetingStatus status, LocalDateTime time);
    List<Meeting> findByStudentIdAndStatusAndScheduledTimeAfter(String studentId, Meeting.MeetingStatus status, LocalDateTime time);
    Optional<Meeting> findByRoomId(String roomId);
}