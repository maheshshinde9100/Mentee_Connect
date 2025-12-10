package com.mahesh.mentee_connect.dto;

import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MentorConverter {

    /**
     * Convert a Mentor entity to a MentorDTO with only student IDs
     */
    public MentorDTO convertToDTO(Mentor mentor) {
        if (mentor == null) {
            return null;
        }
        
        MentorDTO dto = new MentorDTO();
        dto.setId(mentor.getId());
        dto.setUsername(mentor.getUsername());
        dto.setEmail(mentor.getEmail());
        dto.setFirstName(mentor.getFirstName());
        dto.setLastName(mentor.getLastName());
        dto.setPhoneNumber(mentor.getPhoneNumber());
        dto.setRole(mentor.getRole());
        dto.setMentorId(mentor.getMentorId());
        dto.setDepartment(mentor.getDepartment());
        dto.setSpecialization(mentor.getSpecialization());
        dto.setDesignation(mentor.getDesignation());
        dto.setYearsOfExperience(mentor.getYearsOfExperience());
        dto.setMaxStudents(mentor.getMaxStudents());
        
        // Convert full student objects to just IDs
        if (mentor.getAssignedStudents() != null) {
            List<String> studentIds = mentor.getAssignedStudents().stream()
                .map(Student::getId)
                .collect(Collectors.toList());
            dto.setAssignedStudentIds(studentIds);
        }
        return dto;
    }
} 