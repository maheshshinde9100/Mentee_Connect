package com.mahesh.mentee_connect.service;

import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.dto.MenteeUpdateRequest;
import com.mahesh.mentee_connect.dto.StudentMentorDTO;
import org.springframework.data.domain.Page;
import java.util.List;

public interface MentorService {
    Mentor createMentor(Mentor mentor);
    Mentor updateMentor(String id, Mentor mentor);
    void deleteMentor(String id);
    Mentor getMentorById(String id);
    Mentor getMentorByUsername(String username);
    List<Mentor> getAllMentors();
    List<StudentMentorDTO> getMentorStudents(String mentorId);
    Mentor getCurrentMentor();
    boolean hasAvailableSlots(String mentorId);
    int getAssignedStudentsCount(String mentorId);
    Page<Mentor> getAllMentors(int page, int size);
    
    // New method for updating student information by mentor
    Student updateStudentAsMentor(String mentorId, String studentId, MenteeUpdateRequest updateRequest);
    
    // New methods for specific mentee operations
    StudentMentorDTO getMenteeDetails(String mentorId, String studentId);
    StudentMentorDTO updateMenteeDetails(String mentorId, String studentId, MenteeUpdateRequest updateRequest);
} 