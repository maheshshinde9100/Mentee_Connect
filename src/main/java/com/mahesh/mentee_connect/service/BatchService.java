package com.mahesh.mentee_connect.service;

import com.mahesh.mentee_connect.model.Batch;
import com.mahesh.mentee_connect.repository.BatchRepository;
import com.mahesh.mentee_connect.model.Student;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;

@Service
public class BatchService {

    @Autowired
    private BatchRepository batchRepository;
    @Autowired
    private MentorRepository mentorRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Transactional
    public Batch createBatch(Batch batch) {
        return batchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public Page<Batch> getAllBatches(int page, int size) {
        return batchRepository.findAll(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Batch getBatchById(String id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", id));
    }

    @Transactional
    public Batch assignMentorToBatch(String batchId, String mentorId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        mentorRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", mentorId));

        if (batch.getMentorsAssigned() == null) {
            batch.setMentorsAssigned(new ArrayList<>());
        }
        if (!batch.getMentorsAssigned().contains(mentorId)) {
            batch.getMentorsAssigned().add(mentorId);
        }

        return batchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public List<Student> getBatchStudents(String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        return studentRepository.findByBatch(batch.getBatchName());
    }

    @Transactional
    public Batch updateBatch(String id, Batch batchDetails) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", id));

        if (batchDetails.getBatchName() != null) {
            batch.setBatchName(batchDetails.getBatchName());
        }
        if (batchDetails.getStartDate() != null) {
            batch.setStartDate(batchDetails.getStartDate());
        }
        if (batchDetails.getEndDate() != null) {
            batch.setEndDate(batchDetails.getEndDate());
        }
        if (batchDetails.getCourse() != null) {
            batch.setCourse(batchDetails.getCourse());
        }

        // Update mentors assigned if provided
        if (batchDetails.getMentorsAssigned() != null) {
            batch.setMentorsAssigned(batchDetails.getMentorsAssigned());
        }

        // Update students assigned if provided
        if (batchDetails.getStudentsAssigned() != null) {
            batch.setStudentsAssigned(batchDetails.getStudentsAssigned());
        }

        return batchRepository.save(batch);
    }

    @Transactional
    public void deleteBatch(String id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", id));
        batchRepository.delete(batch);
    }

    @Transactional
    public List<Student> assignStudentsToBatch(String batchId, List<String> studentIds) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        if (batch.getStudentsAssigned() == null) {
            batch.setStudentsAssigned(new ArrayList<>());
        }

        List<Student> students = new ArrayList<>();
        for (String studentId : studentIds) {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            // Update student's batch information
            student.setBatch(batch.getBatchName());
            student = studentRepository.save(student);
            students.add(student);
            
            // Add student ID to the batch's studentsAssigned list if not already present
            if (!batch.getStudentsAssigned().contains(studentId)) {
                batch.getStudentsAssigned().add(studentId);
            }
        }
        
        // Save the updated batch
        batchRepository.save(batch);

        return students;
    }
    
    @Transactional(readOnly = true)
    public List<Mentor> getBatchMentors(String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        
        List<Mentor> mentors = new ArrayList<>();
        
        if (batch.getMentorsAssigned() != null && !batch.getMentorsAssigned().isEmpty()) {
            for (String mentorId : batch.getMentorsAssigned()) {
                mentorRepository.findById(mentorId)
                    .ifPresent(mentors::add);
            }
        }
        
        return mentors;
    }
    
    @Transactional
    public Batch assignMentorsToBatch(String batchId, List<String> mentorIds) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        
        // Validate that all mentors exist
        List<Mentor> mentors = new ArrayList<>();
        for (String mentorId : mentorIds) {
            Mentor mentor = mentorRepository.findById(mentorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", mentorId));
            mentors.add(mentor);
        }
        
        // Set the mentors
        batch.setMentorsAssigned(mentorIds != null ? mentorIds : new ArrayList<>());
        
        // Save and return the updated batch
        return batchRepository.save(batch);
    }
}