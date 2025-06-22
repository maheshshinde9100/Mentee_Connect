package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.Meeting;
import com.mahesh.mentee_connect.dto.MenteeUpdateRequest;
import com.mahesh.mentee_connect.dto.StudentMentorDTO;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.repository.MeetingRepository;
import com.mahesh.mentee_connect.service.MentorService;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import com.mahesh.mentee_connect.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MentorServiceImpl implements MentorService {
    
    @Autowired
    private MentorRepository mentorRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private MeetingRepository meetingRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Mentor createMentor(Mentor mentor) {
        mentor.setPassword(passwordEncoder.encode(mentor.getPassword()));
        return mentorRepository.save(mentor);
    }

    @Override
    @Transactional
    public Mentor updateMentor(String id, Mentor mentorDetails) {
        Mentor mentor = getMentorById(id);
        mentor.setFirstName(mentorDetails.getFirstName());
        mentor.setLastName(mentorDetails.getLastName());
        mentor.setEmail(mentorDetails.getEmail());
        mentor.setPhoneNumber(mentorDetails.getPhoneNumber());
        mentor.setDepartment(mentorDetails.getDepartment());
        mentor.setSpecialization(mentorDetails.getSpecialization());
        mentor.setDesignation(mentorDetails.getDesignation());
        mentor.setYearsOfExperience(mentorDetails.getYearsOfExperience());
        mentor.setMaxStudents(mentorDetails.getMaxStudents());
        return mentorRepository.save(mentor);
    }

    @Override
    @Transactional
    public void deleteMentor(String id) {
        Mentor mentor = getMentorById(id);
        mentorRepository.delete(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public Mentor getMentorById(String id) {
        return mentorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Mentor getMentorByUsername(String username) {
        return mentorRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mentor> getAllMentors() {
        return mentorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentMentorDTO> getMentorStudents(String mentorId) {
        List<Student> students = studentRepository.findByAssignedMentorId(mentorId);
        return students.stream()
                .map(student -> new StudentMentorDTO(
                    student.getId(),
                    student.getUsername(),
                    student.getEmail(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getPhoneNumber(),
                    student.getStudentId(),
                    student.getCourse(),
                    student.getBatch(),
                    student.getSemester(),
                    student.getAttendance(),
                    student.getCgpa(),
                    mentorId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meeting> getMentorMeetings(String mentorId) {
        return meetingRepository.findByMentorId(mentorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Mentor getCurrentMentor() {
        // Get the authenticated user from the SecurityContext
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        
        String username = authentication.getName();
        return mentorRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor", "username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAvailableSlots(String mentorId) {
        Mentor mentor = getMentorById(mentorId);
        return getAssignedStudentsCount(mentorId) < mentor.getMaxStudents();
    }

    @Override
    @Transactional(readOnly = true)
    public int getAssignedStudentsCount(String mentorId) {
        return studentRepository.findByAssignedMentorId(mentorId).size();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Mentor> getAllMentors(int page, int size) {
        return mentorRepository.findAll(PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public Student updateStudentAsMentor(String mentorId, String studentId, MenteeUpdateRequest updateRequest) {
        // Verify mentor exists
        Mentor mentor = getMentorById(mentorId);
        
        // Get the student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        
        // Verify that this student is assigned to this mentor
        if (student.getAssignedMentor() == null || !student.getAssignedMentor().getId().equals(mentorId)) {
            throw new UnauthorizedException("This mentor is not authorized to update this student's information");
        }
        
        // Update allowed fields only
        if (updateRequest.getFirstName() != null) {
            student.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            student.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhoneNumber() != null) {
            student.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getCourse() != null) {
            student.setCourse(updateRequest.getCourse());
        }
        if (updateRequest.getSemester() != null) {
            student.setSemester(updateRequest.getSemester());
        }
        if (updateRequest.getCgpa() != null) {
            student.setCgpa(updateRequest.getCgpa());
        }
        
        // Save and return updated student
        return studentRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentMentorDTO getMenteeDetails(String mentorId, String studentId) {
        // Verify mentor exists
        Mentor mentor = getMentorById(mentorId);
        
        // Get the student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        
        // Verify that this student is assigned to this mentor
        if (student.getAssignedMentor() == null || !student.getAssignedMentor().getId().equals(mentorId)) {
            throw new UnauthorizedException("This student is not assigned to the specified mentor");
        }
        
        return new StudentMentorDTO(
            student.getId(),
            student.getUsername(),
            student.getEmail(),
            student.getFirstName(),
            student.getLastName(),
            student.getPhoneNumber(),
            student.getStudentId(),
            student.getCourse(),
            student.getBatch(),
            student.getSemester(),
            student.getAttendance(),
            student.getCgpa(),
            mentorId
        );
    }

    @Override
    @Transactional
    public StudentMentorDTO updateMenteeDetails(String mentorId, String studentId, MenteeUpdateRequest updateRequest) {
        // Verify mentor exists
        Mentor mentor = getMentorById(mentorId);
        
        // Get the student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        
        // Verify that this student is assigned to this mentor
        if (student.getAssignedMentor() == null || !student.getAssignedMentor().getId().equals(mentorId)) {
            throw new UnauthorizedException("This mentor is not authorized to update this student's information");
        }
        
        // Update allowed fields only
        if (updateRequest.getFirstName() != null) {
            student.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            student.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhoneNumber() != null) {
            student.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getCourse() != null) {
            student.setCourse(updateRequest.getCourse());
        }
        if (updateRequest.getSemester() != null) {
            student.setSemester(updateRequest.getSemester());
        }
        if (updateRequest.getCgpa() != null) {
            student.setCgpa(updateRequest.getCgpa());
        }
        if (updateRequest.getDepartment() != null) {
            student.setDepartment(updateRequest.getDepartment());
        }
        if (updateRequest.getEmail() != null) {
            student.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getDob() != null) {
            student.setDob(updateRequest.getDob());
        }
        if (updateRequest.getProfilePhoto() != null) {
            student.setProfilePhoto(updateRequest.getProfilePhoto());
        }
        
        // Save the updated student
        student = studentRepository.save(student);
        
        // Save mentor notes and other mentor-specific updates
        if (updateRequest.getMentorNotes() != null || 
            updateRequest.getSkills() != null || 
            updateRequest.getInterests() != null) {
            
            // Logic to save mentor-specific updates would go here
            // This might involve updating a separate mentor-student relationship entity
        }
        
        // Return the updated student as DTO
        return new StudentMentorDTO(
            student.getId(),
            student.getUsername(),
            student.getEmail(),
            student.getFirstName(),
            student.getLastName(),
            student.getPhoneNumber(),
            student.getStudentId(),
            student.getCourse(),
            student.getBatch(),
            student.getSemester(),
            student.getAttendance(),
            student.getCgpa(),
            mentorId
        );
    }
}