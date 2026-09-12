package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.dto.MenteeUpdateRequest;
import com.mahesh.mentee_connect.dto.StudentMentorDTO;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.service.MentorService;
import com.mahesh.mentee_connect.exception.ResourceNotFoundException;
import com.mahesh.mentee_connect.exception.UnauthorizedException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MentorServiceImpl implements MentorService {

    private static final Logger log = LoggerFactory.getLogger(MentorServiceImpl.class);

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    @Transactional
    @CacheEvict(value = {"mentors", "analytics"}, allEntries = true)
    public Mentor createMentor(Mentor mentor) {
        mentor.setPassword(passwordEncoder.encode(mentor.getPassword()));
        mentor.setRole(com.mahesh.mentee_connect.model.User.UserRole.ROLE_MENTOR);
        return mentorRepository.save(mentor);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"mentors", "analytics"}, allEntries = true)
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
    @CacheEvict(value = {"mentors", "analytics"}, allEntries = true)
    public void deleteMentor(String id) {
        Mentor mentor = getMentorById(id);
        mentorRepository.delete(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "mentors", key = "#id")
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

    private List<Student> safeFindStudentsByMentorId(String mentorId) {
        if (mentorId == null || mentorId.isBlank()) return Collections.emptyList();

        ObjectId mentorOid;
        try {
            mentorOid = new ObjectId(mentorId);
        } catch (IllegalArgumentException e) {
            log.error("safeFindStudentsByMentorId: '{}' is not a valid ObjectId", mentorId);
            return Collections.emptyList();
        }

        // Primary path: repo @Query with ObjectId — Atlas-compatible dot-notation
        try {
            List<Student> s = studentRepository.findByAssignedMentorId(mentorOid);
            if (s != null && !s.isEmpty()) {
                log.debug("findByAssignedMentorId({}) returned {} students via repo @Query", mentorId, s.size());
                return s;
            }
            log.debug("findByAssignedMentorId({}) returned empty, trying MongoTemplate fallback", mentorId);
        } catch (Exception ex) {
            log.warn("StudentRepository.findByAssignedMentorId({}) failed, falling back. {}: {}",
                    mentorId, ex.getClass().getSimpleName(), ex.getMessage());
        }

        // Fallback: Atlas-compatible dot-notation via Criteria (no $ref operator)
        try {
            org.springframework.data.mongodb.core.query.Query q =
                new org.springframework.data.mongodb.core.query.Query(
                    org.springframework.data.mongodb.core.query.Criteria
                        .where("assignedMentor.$id").is(mentorOid));
            List<Student> fallback = mongoTemplate.find(q, Student.class);
            int found = fallback == null ? 0 : fallback.size();
            log.info("MongoTemplate fallback for mentorId={} returned {} students", mentorId, found);
            return fallback == null ? Collections.emptyList() : fallback;
        } catch (Exception fx) {
            log.error("MongoTemplate fallback failed for mentorId={}: {} - {}",
                    mentorId, fx.getClass().getSimpleName(), fx.getMessage(), fx);
            return Collections.emptyList();
        }
    }

    private StudentMentorDTO studentToDTO(Student s, String mentorId) {
        if (s == null) return null;
        try {
            int sem = s.getSemester();
            double att = s.getAttendance();
            double cg = s.getCgpa();
            return new StudentMentorDTO(
                    s.getId(),
                    s.getUsername(),
                    s.getEmail(),
                    s.getFirstName(),
                    s.getLastName(),
                    s.getPhoneNumber(),
                    s.getStudentId(),
                    s.getCourse(),
                    s.getBatch(),
                    sem,
                    att,
                    cg,
                    mentorId);
        } catch (Exception ex) {
            log.warn("Skipping corrupt student document id={}: {} - {}",
                    s.getId(), ex.getClass().getSimpleName(), ex.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentMentorDTO> getMentorStudents(String mentorId) {
        List<Student> students = safeFindStudentsByMentorId(mentorId);
        List<StudentMentorDTO> result = new ArrayList<>(students.size());
        for (Student s : students) {
            StudentMentorDTO dto = studentToDTO(s, mentorId);
            if (dto != null) result.add(dto);
        }
        log.debug("getMentorStudents({}) -> {} DTOs (from {} raw documents)", mentorId, result.size(), students.size());
        return result;
    }

    @Override
    @Transactional
    public Mentor getCurrentMentor() {
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof com.mahesh.mentee_connect.security.services.UserDetailsImpl) {
            com.mahesh.mentee_connect.security.services.UserDetailsImpl ud =
                    (com.mahesh.mentee_connect.security.services.UserDetailsImpl) principal;

            String userId = ud.getId();
            if (userId != null && !userId.isBlank()) {
                java.util.Optional<Mentor> byId = mentorRepository.findById(userId);
                if (byId.isPresent()) {
                    return normalizeMentorUsername(byId.get(), ud);
                }
            }

            String email = ud.getEmail();
            if (email != null && !email.isBlank()) {
                java.util.Optional<Mentor> byEmail = mentorRepository.findByEmail(email);
                if (byEmail.isPresent()) {
                    return normalizeMentorUsername(byEmail.get(), ud);
                }
            }
        }

        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            java.util.Optional<Mentor> byUsername = mentorRepository.findByUsername(name);
            if (byUsername.isPresent()) return byUsername.get();
            java.util.Optional<Mentor> byEmail = mentorRepository.findByEmail(name);
            if (byEmail.isPresent()) return byEmail.get();
        }

        throw new ResourceNotFoundException("Mentor", "principal", String.valueOf(principal));
    }

    private Mentor normalizeMentorUsername(Mentor mentor, com.mahesh.mentee_connect.security.services.UserDetailsImpl ud) {
        boolean dirty = false;
        if ((mentor.getUsername() == null || mentor.getUsername().isBlank()) && ud.getUsername() != null && !ud.getUsername().isBlank()) {
            mentor.setUsername(ud.getUsername());
            dirty = true;
        }
        if (mentor.getEmail() == null && ud.getEmail() != null) {
            mentor.setEmail(ud.getEmail());
            dirty = true;
        }
        if (mentor.getRole() == null) {
            mentor.setRole(com.mahesh.mentee_connect.model.User.UserRole.ROLE_MENTOR);
            dirty = true;
        }
        if (dirty) {
            return mentorRepository.save(mentor);
        }
        return mentor;
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
        try {
            ObjectId oid = new ObjectId(mentorId);
            long c = studentRepository.countByAssignedMentorId(oid);
            return (int) c;
        } catch (Exception ex) {
            log.warn("countByAssignedMentorId({}) failed: {}. Falling back to list size.",
                    mentorId, ex.getMessage());
            return safeFindStudentsByMentorId(mentorId).size();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Mentor> getAllMentors(int page, int size) {
        return mentorRepository.findAll(PageRequest.of(page, size));
    }

    private boolean isStudentAssignedToMentor(Student student, String mentorId) {
        if (student == null || mentorId == null) return false;
        try {
            Mentor ref = student.getAssignedMentor();
            if (ref != null) {
                String refId = ref.getId();
                if (refId != null && refId.equals(mentorId)) return true;
            }
        } catch (Exception ignore) { }
        List<Student> assigned = safeFindStudentsByMentorId(mentorId);
        String sid = student.getId();
        if (sid == null) return false;
        for (Student s : assigned) {
            if (sid.equals(s.getId())) return true;
        }
        return false;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"students", "mentors", "analytics"}, allEntries = true)
    public Student updateStudentAsMentor(String mentorId, String studentId, MenteeUpdateRequest updateRequest) {
        Mentor mentor = getMentorById(mentorId);
        if (mentor == null) throw new ResourceNotFoundException("Mentor", "id", mentorId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!isStudentAssignedToMentor(student, mentorId)) {
            throw new UnauthorizedException("This mentor is not authorized to update this student's information");
        }

        if (updateRequest.getFirstName() != null) student.setFirstName(updateRequest.getFirstName());
        if (updateRequest.getLastName() != null) student.setLastName(updateRequest.getLastName());
        if (updateRequest.getPhoneNumber() != null) student.setPhoneNumber(updateRequest.getPhoneNumber());
        if (updateRequest.getCourse() != null) student.setCourse(updateRequest.getCourse());
        if (updateRequest.getSemester() != null) student.setSemester(updateRequest.getSemester());
        if (updateRequest.getCgpa() != null) student.setCgpa(updateRequest.getCgpa());

        return studentRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentMentorDTO getMenteeDetails(String mentorId, String studentId) {
        Mentor mentor = getMentorById(mentorId);
        if (mentor == null) throw new ResourceNotFoundException("Mentor", "id", mentorId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!isStudentAssignedToMentor(student, mentorId)) {
            throw new UnauthorizedException("This student is not assigned to the specified mentor");
        }

        return Objects.requireNonNullElse(studentToDTO(student, mentorId),
                new StudentMentorDTO(
                        student.getId(),
                        student.getUsername(),
                        student.getEmail(),
                        student.getFirstName(),
                        student.getLastName(),
                        student.getPhoneNumber(),
                        student.getStudentId(),
                        student.getCourse(),
                        student.getBatch(),
                        0,
                        0.0,
                        0.0,
                        mentorId));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"students", "mentors", "analytics"}, allEntries = true)
    public StudentMentorDTO updateMenteeDetails(String mentorId, String studentId, MenteeUpdateRequest updateRequest) {
        Mentor mentor = getMentorById(mentorId);
        if (mentor == null) throw new ResourceNotFoundException("Mentor", "id", mentorId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (!isStudentAssignedToMentor(student, mentorId)) {
            throw new UnauthorizedException("This mentor is not authorized to update this student's information");
        }

        if (updateRequest.getFirstName() != null) student.setFirstName(updateRequest.getFirstName());
        if (updateRequest.getLastName() != null) student.setLastName(updateRequest.getLastName());
        if (updateRequest.getPhoneNumber() != null) student.setPhoneNumber(updateRequest.getPhoneNumber());
        if (updateRequest.getCourse() != null) student.setCourse(updateRequest.getCourse());
        if (updateRequest.getSemester() != null) student.setSemester(updateRequest.getSemester());
        if (updateRequest.getCgpa() != null) student.setCgpa(updateRequest.getCgpa());
        if (updateRequest.getDepartment() != null) student.setDepartment(updateRequest.getDepartment());
        if (updateRequest.getEmail() != null) student.setEmail(updateRequest.getEmail());
        if (updateRequest.getDob() != null) student.setDob(updateRequest.getDob());
        if (updateRequest.getProfilePhoto() != null) student.setProfilePhoto(updateRequest.getProfilePhoto());

        student = studentRepository.save(student);

        StudentMentorDTO dto = studentToDTO(student, mentorId);
        if (dto != null) return dto;
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
                0,
                0.0,
                0.0,
                mentorId);
    }
}