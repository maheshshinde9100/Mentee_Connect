package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.MenteeTask;
import com.mahesh.mentee_connect.model.MenteeAttendance;
import com.mahesh.mentee_connect.dto.MentorResponse;
import com.mahesh.mentee_connect.dto.ProgressReport;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.MenteeTaskRepository;
import com.mahesh.mentee_connect.repository.MenteeAttendanceRepository;
import com.mahesh.mentee_connect.service.StudentService;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import com.mahesh.mentee_connect.dto.StudentDTO;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import com.mahesh.mentee_connect.dto.CertificateDTO;
import com.mahesh.mentee_connect.dto.StudentUpdateDTO;
import com.mahesh.mentee_connect.model.Certificate;
import com.mahesh.mentee_connect.repository.CertificateRepository;
import com.mahesh.mentee_connect.exception.UnauthorizedException;
import com.mahesh.mentee_connect.dto.StudentProfileDTO;
import com.mahesh.mentee_connect.dto.StudentProfileResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StudentServiceImpl implements StudentService {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private MentorRepository mentorRepository;
    
    @Autowired
    private MenteeTaskRepository menteeTaskRepository;
    
    @Autowired
    private MenteeAttendanceRepository menteeAttendanceRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CertificateRepository certificateRepository;

    @Value("${app.upload.dir:uploads/certificates}")
    private String uploadDir;

    @Override
    @Transactional
    public Student createStudent(Student student) {
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        student.setRole(com.mahesh.mentee_connect.model.User.UserRole.ROLE_STUDENT);
        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public Student updateStudent(String id, Student studentDetails) {
        Student student = getStudentById(id);
        student.setFirstName(studentDetails.getFirstName());
        student.setLastName(studentDetails.getLastName());
        student.setEmail(studentDetails.getEmail());
        student.setPhoneNumber(studentDetails.getPhoneNumber());
        student.setCourse(studentDetails.getCourse());
        student.setBatch(studentDetails.getBatch());
        student.setSemester(studentDetails.getSemester());
        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public void deleteStudent(String id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Student getStudentById(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Student getStudentByUsername(String username) {
        return studentRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByMentorId(String mentorId) {
        return studentRepository.findByAssignedMentorId(mentorId);
    }

    @Override
    @Transactional
    public Student assignMentorToStudent(String studentId, String mentorId) {
        Student student = getStudentById(studentId);
        Mentor mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", mentorId));
        
        // Check if mentor has available slots
        if (mentor.getAssignedStudents().size() >= mentor.getMaxStudents()) {
            throw new RuntimeException("Mentor has reached maximum student capacity");
        }

        // Update student's mentor
        student.setAssignedMentor(mentor);
        student = studentRepository.save(student);

        // Update mentor's assigned students list
        if (!mentor.getAssignedStudents().contains(student)) {
            mentor.getAssignedStudents().add(student);
            mentorRepository.save(mentor);
        }

        return student;
    }

    @Override
    @Transactional
    public Student updateStudentProgress(String studentId, double attendance, double cgpa) {
        Student student = getStudentById(studentId);
        student.setAttendance(attendance);
        student.setCgpa(cgpa);
        return studentRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Student getCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCurrentStudent(auth);
    }

    @Override
    @Transactional(readOnly = true)
    public Student getCurrentStudent(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Authentication is null or not authenticated");
            return null;
        }
        String username = authentication.getName();
        return studentRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public MentorResponse getMentorResponseByStudentId(String studentId) {
        Student student = getStudentById(studentId);
        if (student.getAssignedMentor() == null) {
            throw new ResourceNotFoundException("Mentor not assigned to student", "studentId", studentId);
        }
        Mentor mentor = student.getAssignedMentor();
        return new MentorResponse(mentor.getId(), mentor.getFirstName(), mentor.getLastName(), 
            mentor.getEmail(), mentor.getPhoneNumber(), mentor.getDepartment(), mentor.getSpecialization());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Student> getAllStudentsPageable(int page, int size) {
        return studentRepository.findAll(PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> searchStudents(String query) {
        return studentRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
            query, query, query);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorResponse getMyMentor(String studentId) {
        Student student = getStudentById(studentId);
        if (student.getAssignedMentor() == null) {
            throw new ResourceNotFoundException("Mentor not assigned to student", "studentId", studentId);
        }
        Mentor mentor = student.getAssignedMentor();
        return new MentorResponse(mentor.getId(), mentor.getFirstName(), mentor.getLastName(),
            mentor.getEmail(), mentor.getPhoneNumber(), mentor.getDepartment(), mentor.getSpecialization());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressReport> getProgressReports(String studentId) {
        Student student = getStudentById(studentId);
        
        // Retrieve tasks and attendance data for the student
        List<MenteeTask> tasks = menteeTaskRepository.findByStudentId(studentId);
        List<MenteeAttendance> attendanceRecords = menteeAttendanceRepository.findByStudentId(studentId);
        
        // Create a progress report based on actual data
        ProgressReport currentReport = new ProgressReport();
        currentReport.setId(UUID.randomUUID().toString());
        currentReport.setStudentId(studentId);
        currentReport.setStudentName(student.getFirstName() + " " + student.getLastName());
        currentReport.setReportDate(LocalDateTime.now());
        currentReport.setAttendance(student.getAttendance());
        currentReport.setCgpa(student.getCgpa());
        
        // Calculate task completion rate
        if (!tasks.isEmpty()) {
            long completedTasks = tasks.stream()
                    .filter(task -> task.getStatus() == MenteeTask.TaskStatus.COMPLETED)
                    .count();
            double completionRate = (double) completedTasks / tasks.size() * 100.0;
            currentReport.setTaskCompletionRate(Math.round(completionRate * 10.0) / 10.0); // Round to 1 decimal place
        }
        
        // Calculate recent attendance rate (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<MenteeAttendance> recentAttendance = attendanceRecords.stream()
                .filter(a -> a.getDate().isAfter(thirtyDaysAgo.toLocalDate()))
                .collect(Collectors.toList());
        
        if (!recentAttendance.isEmpty()) {
            long presentCount = recentAttendance.stream()
                    .filter(a -> a.getStudentStatus(studentId) == MenteeAttendance.AttendanceStatus.PRESENT)
                    .count();
            double attendanceRate = (double) presentCount / recentAttendance.size() * 100.0;
            currentReport.setRecentAttendanceRate(Math.round(attendanceRate * 10.0) / 10.0); // Round to 1 decimal place
        }
        
        return List.of(currentReport);
    }

    @Override
    public List<StudentDTO> getAllStudentsAsDTO() {
        return studentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private StudentDTO convertToDTO(Student student) {
        return StudentDTO.builder()
                .id(student.getId())
                .username(student.getUsername())
                .email(student.getEmail())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .rollNumber(student.getStudentId())
                .branch(student.getCourse())
                .cgpa(student.getCgpa())
                .mentorId(student.getAssignedMentor() != null ? student.getAssignedMentor().getId() : null)
                .mentorName(student.getAssignedMentor() != null ? student.getAssignedMentor().getFirstName() + " " + student.getAssignedMentor().getLastName() : null)
                .phoneNumber(student.getPhoneNumber())
                .build();
    }

    @Override
    @Transactional
    public List<Student> assignMentorToMultipleStudents(List<String> studentIds, String mentorId) {
        Mentor mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", mentorId));
        
        // Check if mentor has enough available slots
        int currentAssignedCount = mentor.getAssignedStudents().size();
        int requestedAssignments = studentIds.size();
        
        if (currentAssignedCount + requestedAssignments > mentor.getMaxStudents()) {
            throw new RuntimeException("Mentor does not have enough available slots. " +
                    "Current: " + currentAssignedCount + ", Requested: " + requestedAssignments + 
                    ", Maximum: " + mentor.getMaxStudents());
        }

        List<Student> updatedStudents = new ArrayList<>();
        
        for (String studentId : studentIds) {
            Student student = getStudentById(studentId);
            
            // Update student's mentor
            student.setAssignedMentor(mentor);
            student = studentRepository.save(student);
            updatedStudents.add(student);

            // Add student to mentor's assigned students list if not already there
            if (!mentor.getAssignedStudents().contains(student)) {
                mentor.getAssignedStudents().add(student);
            }
        }
        
        // Save the mentor with updated student list
        mentorRepository.save(mentor);
        
        return updatedStudents;
    }

    @Override
    @Transactional
    public Student removeMentorFromStudent(String studentId) {
        Student student = getStudentById(studentId);
        Mentor mentor = student.getAssignedMentor();
        
        if (mentor != null) {
            // Remove student from mentor's list
            mentor.getAssignedStudents().removeIf(s -> s.getId().equals(studentId));
            mentorRepository.save(mentor);
            
            // Remove mentor from student
            student.setAssignedMentor(null);
            student = studentRepository.save(student);
        }
        
        return student;
    }

    @Override
    public CertificateDTO uploadCertificate(String studentId, MultipartFile file, String description) {
        try {
            // Verify student exists
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Create certificate record
            Certificate certificate = new Certificate();
            certificate.setStudentId(studentId);
            certificate.setCertificateName(originalFilename);
            certificate.setCertificateUrl("/api/certificates/" + filename);
            certificate.setFilename(filename);
            certificate.setDescription(description);
            certificate.setUploadDate(LocalDateTime.now());
            certificate.setStatus("PENDING");

            // Save certificate to database
            certificate = certificateRepository.save(certificate);

            // Convert to DTO
            return convertToDTO(certificate);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public List<CertificateDTO> getStudentCertificates(String studentId) {
        // Verify student exists
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }

        // Get certificates from database
        List<Certificate> certificates = certificateRepository.findByStudentId(studentId);
        return certificates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCertificate(String studentId, String certificateId) {
        // Verify student exists
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }

        // Get certificate from database
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + certificateId));

        // Verify certificate belongs to student
        if (!certificate.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("Certificate does not belong to student");
        }

        // Delete file from storage
        Path filePath = Paths.get(uploadDir).resolve(certificate.getFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }

        // Delete certificate from database
        certificateRepository.delete(certificate);
    }

    private CertificateDTO convertToDTO(Certificate certificate) {
        CertificateDTO dto = new CertificateDTO();
        dto.setId(certificate.getId());
        dto.setStudentId(certificate.getStudentId());
        dto.setCertificateName(certificate.getCertificateName());
        dto.setCertificateUrl(certificate.getCertificateUrl());
        dto.setUploadDate(certificate.getUploadDate());
        dto.setDescription(certificate.getDescription());
        dto.setStatus(certificate.getStatus());
        return dto;
    }

    @Override
    public Page<StudentProfileDTO> getStudentProfiles(int page, int size) {
        Page<Student> studentsPage = studentRepository.findAll(PageRequest.of(page, size));
        return studentsPage.map(this::convertToProfileDTO);
    }

    @Override
    public StudentProfileDTO getStudentProfileById(String id) {
        Student student = getStudentById(id);
        return convertToProfileDTO(student);
    }

    @Override
    public StudentProfileDTO updateStudentProfile(String studentId, StudentUpdateDTO updateDTO) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        student.setFirstName(updateDTO.getFirstName());
        student.setLastName(updateDTO.getLastName());
        student.setEmail(updateDTO.getEmail());
        student.setPhoneNumber(updateDTO.getPhoneNumber());
        student.setCourse(updateDTO.getCourse());
        student.setBatch(updateDTO.getBatch());
        student.setSemester(updateDTO.getSemester());

        student = studentRepository.save(student);
        return convertToProfileDTO(student);
    }

    private StudentProfileDTO convertToProfileDTO(Student student) {
        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setId(student.getId());
        dto.setStudentId(student.getStudentId());
        dto.setUsername(student.getUsername());
        dto.setEmail(student.getEmail());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setPhoneNumber(student.getPhoneNumber());
        dto.setCourse(student.getCourse());
        dto.setBatch(student.getBatch());
        dto.setSemester(student.getSemester());
        dto.setAttendance(student.getAttendance());
        dto.setCgpa(student.getCgpa());
        
        // Only set mentor ID instead of full mentor object
        if (student.getAssignedMentor() != null) {
            dto.setMentorId(student.getAssignedMentor().getId());
        }
        
        return dto;
    }
}