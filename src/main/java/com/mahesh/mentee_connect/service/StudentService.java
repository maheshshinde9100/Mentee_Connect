package com.mahesh.mentee_connect.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.dto.MentorResponse;
import com.mahesh.mentee_connect.dto.ProgressReport;
import com.mahesh.mentee_connect.dto.StudentDTO;
import com.mahesh.mentee_connect.dto.CertificateDTO;
import com.mahesh.mentee_connect.dto.StudentUpdateDTO;
import com.mahesh.mentee_connect.dto.StudentProfileDTO;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.repository.MentorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface StudentService {

    Student createStudent(Student student);
    Student updateStudent(String id, Student student);
    void deleteStudent(String id);
    Student getStudentById(String id);
    Student getStudentByUsername(String username);
    List<Student> getAllStudents();
    Page<Student> getAllStudentsPageable(int page, int size);
    List<Student> getStudentsByMentorId(String mentorId);
    Student assignMentorToStudent(String studentId, String mentorId);
    List<Student> assignMentorToMultipleStudents(List<String> studentIds, String mentorId);
    Student updateStudentProgress(String id, double attendance, double cgpa);
    Student getCurrentStudent();
    Student getCurrentStudent(Authentication authentication);
    MentorResponse getMentorResponseByStudentId(String studentId);
    Student getStudentByEmail(String email);
    List<Student> searchStudents(String query);
    MentorResponse getMyMentor(String studentId);
    List<ProgressReport> getProgressReports(String studentId);
    List<StudentDTO> getAllStudentsAsDTO();
    
    // Certificate management methods
    CertificateDTO uploadCertificate(String studentId, MultipartFile file, String description);
    List<CertificateDTO> getStudentCertificates(String studentId);
    void deleteCertificate(String studentId, String certificateId);
    
    // Student profile methods with pagination
    Page<StudentProfileDTO> getStudentProfiles(int page, int size);
    StudentProfileDTO getStudentProfileById(String id);
    StudentProfileDTO updateStudentProfile(String studentId, StudentUpdateDTO updateDTO);
}