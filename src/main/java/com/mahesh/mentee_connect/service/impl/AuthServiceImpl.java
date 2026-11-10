package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.payload.request.LoginRequest;
import com.mahesh.mentee_connect.payload.request.RegisterRequest;
import com.mahesh.mentee_connect.payload.response.JwtResponse;
import com.mahesh.mentee_connect.repository.AdminRepository;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.security.jwt.JwtUtils;
import com.mahesh.mentee_connect.security.services.UserDetailsImpl;
import com.mahesh.mentee_connect.security.services.UserDetailsServiceImpl;
import com.mahesh.mentee_connect.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        String refreshToken = jwtUtils.generateTokenFromUserDetails(userDetails);

        return new JwtResponse(
                accessToken,
                refreshToken,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles);
    }

    @Override
    @Transactional
    public JwtResponse registerUser(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail() == null ? "" : registerRequest.getEmail().trim().toLowerCase();
        String fullName = registerRequest.getFullName() == null ? "" : registerRequest.getFullName().trim();
        String rawPassword = registerRequest.getPassword();

        if (email.isEmpty()) {
            throw new IllegalArgumentException("email: must not be blank");
        }
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("fullName: must not be blank");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("password: must be at least 6 characters");
        }

        // Registration is open to students and mentors only. Admins are provisioned internally.
        User.UserRole role = resolveRegistrationRole(registerRequest.getRole());

        if (studentRepository.existsByEmail(email)
                || mentorRepository.existsByEmail(email)
                || adminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        String[] nameParts = splitFullName(fullName);
        LocalDateTime now = LocalDateTime.now();

        User savedUser;
        if (role == User.UserRole.ROLE_MENTOR) {
            Mentor mentor = new Mentor();
            applyRegistrationFields(mentor, email, encodedPassword, nameParts[0], nameParts[1], role, now);
            mentor.setMentorId(generateUniqueMentorId());
            savedUser = mentorRepository.save(mentor);
        } else {
            Student student = new Student();
            applyRegistrationFields(student, email, encodedPassword, nameParts[0], nameParts[1], role, now);
            student.setStudentId(generateUniqueStudentId());
            student.setAttendance(0.0);
            student.setCgpa(0.0);
            savedUser = studentRepository.save(student);
            assignAvailableMentor((Student) savedUser);
        }

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);
        String accessToken = jwtUtils.generateTokenFromUserDetails(userDetails);
        String refreshToken = jwtUtils.generateTokenFromUserDetails(userDetails);

        logger.info("Registered new {} account: id={} email={}", role, savedUser.getId(), email);

        return new JwtResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                List.of(role.name()),
                (int) (jwtUtils.getJwtExpirationMs() / 1000));
    }

    private User.UserRole resolveRegistrationRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new IllegalArgumentException("role: must not be blank");
        }
        String normalized = rawRole.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return switch (normalized) {
            case "STUDENT", "MENTEE" -> User.UserRole.ROLE_STUDENT;
            case "MENTOR" -> User.UserRole.ROLE_MENTOR;
            default -> throw new IllegalArgumentException(
                    "role: unsupported role '" + rawRole + "'. Allowed values: STUDENT, MENTEE, MENTOR");
        };
    }

    private void applyRegistrationFields(User user, String email, String encodedPassword,
            String firstName, String lastName, User.UserRole role, LocalDateTime now) {
        // Email doubles as the unique username — it is already unique across all user collections.
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
    }

    private String[] splitFullName(String fullName) {
        int spaceIndex = fullName.indexOf(' ');
        if (spaceIndex < 0) {
            return new String[] { fullName, "" };
        }
        return new String[] { fullName.substring(0, spaceIndex), fullName.substring(spaceIndex + 1).trim() };
    }

    private String generateUniqueStudentId() {
        String studentId;
        do {
            studentId = "STU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (studentRepository.existsByStudentId(studentId));
        return studentId;
    }

    private String generateUniqueMentorId() {
        String mentorId;
        do {
            mentorId = "MEN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (mentorRepository.existsByMentorId(mentorId));
        return mentorId;
    }

    /**
     * Assigns the first mentor that still has a free slot, mirroring
     * StudentService.assignMentorToStudent and DatabaseCleanupRunner. A student without a
     * mentor makes GET /students/my-mentor return 404, which breaks the student dashboard.
     * Best effort only — registration must still succeed if no mentor has capacity.
     */
    private void assignAvailableMentor(Student student) {
        try {
            for (Mentor mentor : mentorRepository.findAll()) {
                List<Student> assigned = mentor.getAssignedStudents();
                int current = assigned == null ? 0 : assigned.size();
                if (current >= mentor.getMaxStudents()) {
                    continue;
                }

                student.setAssignedMentor(mentor);
                studentRepository.save(student);

                if (assigned == null) {
                    assigned = new ArrayList<>();
                    mentor.setAssignedStudents(assigned);
                }
                assigned.add(student);
                mentorRepository.save(mentor);

                logger.info("Auto-assigned mentor {} to student {}", mentor.getId(), student.getId());
                return;
            }
            logger.warn("No mentor with a free slot available for student {}", student.getId());
        } catch (Exception e) {
            logger.error("Failed to auto-assign a mentor to student {}", student.getId(), e);
        }
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
    }

    @Override
    public String getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    @Override
    public boolean isCurrentUserAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    @Override
    public boolean isCurrentUserMentor() {
        return hasRole("ROLE_MENTOR");
    }

    @Override
    public boolean isCurrentUserStudent() {
        return hasRole("ROLE_STUDENT");
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    @Override
    public UserDetails loadUserById(String userId) {
        return userDetailsService.loadUserById(userId);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws org.springframework.security.core.userdetails.UsernameNotFoundException {
        return userDetailsService.loadUserByUsername(username);
    }
} 