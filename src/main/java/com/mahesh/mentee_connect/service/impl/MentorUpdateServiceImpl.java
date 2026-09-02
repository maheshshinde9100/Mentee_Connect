package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.MentorUpdate;
import com.mahesh.mentee_connect.repository.MentorUpdateRepository;
import com.mahesh.mentee_connect.service.MentorUpdateService;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class MentorUpdateServiceImpl implements MentorUpdateService {

    private static final Logger log = LoggerFactory.getLogger(MentorUpdateServiceImpl.class);

    @Autowired
    private MentorUpdateRepository mentorUpdateRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public MentorUpdate createUpdate(MentorUpdate update) {
        update.setCreatedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        return mentorUpdateRepository.save(update);
    }

    @Override
    @Transactional
    public MentorUpdate updateUpdate(String updateId, MentorUpdate updateDetails) {
        MentorUpdate update = getUpdateById(updateId);
        
        // Update fields
        update.setTitle(updateDetails.getTitle());
        update.setContent(updateDetails.getContent());
        update.setStudentIds(updateDetails.getStudentIds());
        update.setAttachmentUrls(updateDetails.getAttachmentUrls());
        update.setImportant(updateDetails.isImportant());
        update.setUpdatedAt(LocalDateTime.now());
        
        return mentorUpdateRepository.save(update);
    }

    @Override
    @Transactional
    public void deleteUpdate(String updateId) {
        MentorUpdate update = getUpdateById(updateId);
        mentorUpdateRepository.delete(update);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorUpdate getUpdateById(String updateId) {
        return mentorUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Update", "id", updateId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorUpdate> getUpdatesByMentor(String mentorId) {
        try {
            return mentorUpdateRepository.findByMentorId(mentorId);
        } catch (Exception ex) {
            log.error("getUpdatesByMentor({}) failed: {} - {}", mentorId,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorUpdate> getUpdatesByStudent(String studentId) {
        Exception primary = null;
        try {
            return mentorUpdateRepository.findByStudentId(studentId);
        } catch (Exception ex) {
            primary = ex;
            log.warn("mentorUpdateRepo.findByStudentId({}) failed @Query List-contains derivation, using MongoTemplate fallback. {}: {}",
                    studentId, ex.getClass().getSimpleName(), ex.getMessage());
        }
        try {
            Document q = new Document("studentIds", studentId);
            List<MentorUpdate> fb = mongoTemplate.find(new BasicQuery(q), MentorUpdate.class);
            return fb == null ? Collections.emptyList() : fb;
        } catch (Exception fx) {
            log.error("Both mentorUpdateRepo and MongoTemplate findByStudentId({}) failed. repo: {}; fallback: {}. Return empty.",
                    studentId, primary == null ? "<n/a>" : primary.getMessage(), fx.getMessage(), fx);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorUpdate> getImportantUpdates() {
        try {
            return mentorUpdateRepository.findByIsImportantTrue();
        } catch (Exception ex) {
            log.error("getImportantUpdates() failed: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorUpdate> getUpdatesByMentorAndStudent(String mentorId, String studentId) {
        Exception primary = null;
        try {
            return mentorUpdateRepository.findByMentorIdAndStudentId(mentorId, studentId);
        } catch (Exception ex) {
            primary = ex;
            log.warn("mentorUpdateRepo.findByMentorIdAndStudentId({}, {}) failed derivation, MongoTemplate fallback. {}: {}",
                    mentorId, studentId, ex.getClass().getSimpleName(), ex.getMessage());
        }
        try {
            Document q = new Document("mentorId", mentorId).append("studentIds", studentId);
            List<MentorUpdate> fb = mongoTemplate.find(new BasicQuery(q), MentorUpdate.class);
            return fb == null ? Collections.emptyList() : fb;
        } catch (Exception fx) {
            log.error("Both mentorUpdateRepo and MongoTemplate findByMentorIdAndStudentId({}, {}) failed. repo: {}; fallback: {}. Return empty.",
                    mentorId, studentId,
                    primary == null ? "<n/a>" : primary.getMessage(),
                    fx.getMessage(), fx);
            return Collections.emptyList();
        }
    }
} 