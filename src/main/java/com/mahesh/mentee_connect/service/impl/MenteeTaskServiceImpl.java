package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.MenteeTask;
import com.mahesh.mentee_connect.repository.MenteeTaskRepository;
import com.mahesh.mentee_connect.service.MenteeTaskService;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class MenteeTaskServiceImpl implements MenteeTaskService {

    private static final Logger log = LoggerFactory.getLogger(MenteeTaskServiceImpl.class);

    @Autowired
    private MenteeTaskRepository menteeTaskRepository;

    @Override
    @Transactional
    public MenteeTask createTask(MenteeTask task) {
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return menteeTaskRepository.save(task);
    }

    @Override
    @Transactional
    public MenteeTask updateTask(String taskId, MenteeTask taskDetails) {
        MenteeTask task = getTaskById(taskId);
        
        // Update fields
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setDueDate(taskDetails.getDueDate());
        task.setStatus(taskDetails.getStatus());
        task.setFeedback(taskDetails.getFeedback());
        task.setUpdatedAt(LocalDateTime.now());
        
        return menteeTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        MenteeTask task = getTaskById(taskId);
        menteeTaskRepository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public MenteeTask getTaskById(String taskId) {
        return menteeTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenteeTask> getTasksByMentor(String mentorId) {
        try {
            return menteeTaskRepository.findByMentorId(mentorId);
        } catch (Exception ex) {
            log.error("getTasksByMentor({}) failed: {} - {}", mentorId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenteeTask> getTasksByStudent(String studentId) {
        try {
            return menteeTaskRepository.findByStudentId(studentId);
        } catch (Exception ex) {
            log.error("getTasksByStudent({}) failed: {} - {}", studentId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenteeTask> getTasksByMentorAndStudent(String mentorId, String studentId) {
        try {
            return menteeTaskRepository.findByMentorIdAndStudentId(mentorId, studentId);
        } catch (Exception ex) {
            log.error("getTasksByMentorAndStudent({}, {}) failed: {} - {}", mentorId, studentId,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenteeTask> getOverdueTasks() {
        try {
            return menteeTaskRepository.findByDueDateBefore(LocalDate.now());
        } catch (Exception ex) {
            log.error("getOverdueTasks() failed: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenteeTask> getTasksByStatus(MenteeTask.TaskStatus status) {
        try {
            return menteeTaskRepository.findByStatus(status);
        } catch (Exception ex) {
            log.error("getTasksByStatus({}) failed: {} - {}", status, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
} 