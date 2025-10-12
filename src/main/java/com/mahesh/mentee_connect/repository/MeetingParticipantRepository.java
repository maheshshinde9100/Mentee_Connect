package com.mahesh.mentee_connect.repository;

import com.mahesh.mentee_connect.model.MeetingParticipant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends MongoRepository<MeetingParticipant, String> {
    List<MeetingParticipant> findByRoomId(String roomId);
    List<MeetingParticipant> findByRoomIdAndStatus(String roomId, MeetingParticipant.ParticipantStatus status);
    List<MeetingParticipant> findByMeetingId(String meetingId);
    Optional<MeetingParticipant> findByRoomIdAndParticipantId(String roomId, String participantId);
    List<MeetingParticipant> findByParticipantId(String participantId);
}