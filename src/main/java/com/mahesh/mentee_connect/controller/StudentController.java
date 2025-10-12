package com.mahesh.mentee_connect.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.mahesh.mentee_connect.dto.*;
import com.mahesh.mentee_connect.model.Meeting;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.service.MeetingService;
import com.mahesh.mentee_connect.service.StudentService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping({"/students", "/api/students"})
@Tag(name = "Students", description = "Student management APIs")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;
    
    @Autowired
    private MeetingService meetingService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create student", description = "Create a new student (Admin only)")
    public ResponseEntity<Student> createStudent(@Valid @RequestBody Student student) {
        return ResponseEntity.ok(studentService.createStudent(student));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    @Operation(summary = "Update student", description = "Update an existing student (Admin or self)")
    public ResponseEntity<Student> updateStudent(@PathVariable String id, @Valid @RequestBody Student student) {
        return ResponseEntity.ok(studentService.updateStudent(id, student));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete student", description = "Delete a student (Admin only)")
    public ResponseEntity<?> deleteStudent(@PathVariable String id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR') or @securityService.isCurrentUser(#id)")
    @Operation(summary = "Get student", description = "Get student by ID (Admin, Mentor, or self)")
    public ResponseEntity<Student> getStudent(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR')")
    @Operation(summary = "Get all students", description = "Get all students (Admin or Mentor)")
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/mentor/{mentorId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentMentor(#mentorId)")
    @Operation(summary = "Get mentor's students", description = "Get all students assigned to a mentor (Admin or mentor)")
    public ResponseEntity<List<Student>> getStudentsByMentor(@PathVariable String mentorId) {
        return ResponseEntity.ok(studentService.getStudentsByMentorId(mentorId));
    }

    @GetMapping("/{id}/meetings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR') or @securityService.isCurrentUser(#id)")
    @Operation(summary = "Get student meetings", description = "Get all meetings for a student (Admin, Mentor, or self)")
    public ResponseEntity<List<Meeting>> getStudentMeetings(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getStudentMeetings(id));
    }

    @PutMapping("/{studentId}/mentor/{mentorId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign mentor", description = "Assign a mentor to a student (Admin only)")
    public ResponseEntity<Student> assignMentorToStudent(
            @PathVariable String studentId,
            @PathVariable String mentorId) {
        return ResponseEntity.ok(studentService.assignMentorToStudent(studentId, mentorId));
    }

    @PutMapping("/{id}/progress")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR')")
    @Operation(summary = "Update progress", description = "Update student's progress (Admin or Mentor)")
    public ResponseEntity<Student> updateStudentProgress(
            @PathVariable String id,
            @RequestParam double attendance,
            @RequestParam double cgpa) {
        return ResponseEntity.ok(studentService.updateStudentProgress(id, attendance, cgpa));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get current student", description = "Get current logged-in student's details")
    public ResponseEntity<Student> getCurrentStudent() {
        return ResponseEntity.ok(studentService.getCurrentStudent());
    }

    @GetMapping("/my-mentor")
    public ResponseEntity<MentorResponse> getMyMentor(Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(studentService.getMyMentor(studentId));
    }

    @GetMapping("/meetings/upcoming")
    public ResponseEntity<List<Meeting>> getUpcomingMeetings(Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(meetingService.getUpcomingMeetingsByStudent(studentId));
    }

    @GetMapping("/meetings/{meetingId}")
    public ResponseEntity<Meeting> getMeetingDetails(
            @PathVariable String meetingId,
            Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(meetingService.getMeetingDetailsForStudent(meetingId, studentId));
    }

    @PostMapping("/meetings/{meetingId}/request-changes")
    public ResponseEntity<MeetingChangeRequest> requestMeetingChanges(
            @PathVariable String meetingId,
            @RequestBody MeetingChangeRequest request,
            Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(
                meetingService.requestMeetingChanges(meetingId, studentId, request));
    }

    @GetMapping("/progress-reports")
    public ResponseEntity<List<ProgressReport>> getProgressReports(Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(studentService.getProgressReports(studentId));
    }

    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student profile", description = "Get current student's profile information")
    public ResponseEntity<StudentProfileDTO> getMyProfile(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(studentService.getStudentProfileById(studentId));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Update student profile", description = "Update current student's profile information")
    public ResponseEntity<StudentProfileDTO> updateMyProfile(
            @Valid @RequestBody StudentUpdateDTO updateDTO,
            Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(studentService.updateStudentProfile(studentId, updateDTO));
    }

    @GetMapping("/profiles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MENTOR')")
    @Operation(summary = "Get all student profiles", description = "Get paginated list of student profiles")
    public ResponseEntity<StudentProfileResponseDTO> getAllStudentProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<StudentProfileDTO> profilesPage = studentService.getStudentProfiles(page, size);
        return ResponseEntity.ok(new StudentProfileResponseDTO(profilesPage));
    }

    @PostMapping("/me/certificates")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Upload certificate", description = "Upload a new certificate document")
    public ResponseEntity<CertificateDTO> uploadCertificate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            Authentication authentication) {
        logger.debug("Received certificate upload request for file: {}", file.getOriginalFilename());
        try {
            Student currentStudent = studentService.getCurrentStudent(authentication);
            if (currentStudent == null) {
                logger.error("No authenticated student found");
                throw new RuntimeException("No authenticated student found");
            }
            String studentId = currentStudent.getId();
            logger.debug("Processing certificate upload for student: {}", studentId);
            
            CertificateDTO certificate = studentService.uploadCertificate(studentId, file, description);
            logger.debug("Certificate uploaded successfully: {}", certificate.getId());
            
            return ResponseEntity.ok(certificate);
        } catch (Exception e) {
            logger.error("Error uploading certificate: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/me/certificates")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student certificates", description = "Get all certificates uploaded by the student")
    public ResponseEntity<List<CertificateDTO>> getMyCertificates(Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        List<CertificateDTO> certificates = studentService.getStudentCertificates(studentId);
        return ResponseEntity.ok(certificates);
    }

    @DeleteMapping("/me/certificates/{certificateId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Delete certificate", description = "Delete a certificate document")
    public ResponseEntity<Void> deleteCertificate(
            @PathVariable String certificateId,
            Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        studentService.deleteCertificate(studentId, certificateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/mentor")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get mentor information", description = "Get information about the student's assigned mentor")
    public ResponseEntity<MentorResponse> getMyMentorInfo(Authentication authentication) {
        String studentId = ((Student) studentService.getCurrentStudent(authentication)).getId();
        return ResponseEntity.ok(studentService.getMyMentor(studentId));
    }
}