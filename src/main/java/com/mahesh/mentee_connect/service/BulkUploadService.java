package com.mahesh.mentee_connect.service;

import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.repository.AdminRepository;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CSV bulk import for mentors and students. Excel files are intentionally not
 * accepted: the deployed build has no spreadsheet parser dependency.
 */
@Service
public class BulkUploadService {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadService.class);
    private static final String DEFAULT_PASSWORD = "Mentee@123";

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> uploadMentors(MultipartFile file) throws IOException {
        return importCsv(file, true);
    }

    public Map<String, Object> uploadStudents(MultipartFile file) throws IOException {
        return importCsv(file, false);
    }

    private Map<String, Object> importCsv(MultipartFile file, boolean mentors) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int total = 0;

        List<String> lines = readLines(file);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }

        char delimiter = detectDelimiter(lines.get(0));
        List<String> headers = parseLine(lines.get(0), delimiter).stream()
                .map(this::normalizeHeader)
                .toList();

        if (headers.isEmpty() || !headers.contains("email")) {
            throw new IllegalArgumentException("Missing header row. First line must contain column names including 'email'");
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            total++;
            int rowNumber = i + 1;
            try {
                Map<String, String> row = toRow(headers, parseLine(line, delimiter));
                if (mentors) {
                    importMentorRow(row, errors, rowNumber);
                } else {
                    importStudentRow(row, errors, rowNumber);
                }
                success++;
            } catch (Exception e) {
                errors.add("Row " + rowNumber + ": " + (e.getMessage() == null ? "invalid data" : e.getMessage()));
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("success", success);
        summary.put("failed", total - success);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", String.format("%s import finished: %d of %d records added",
                mentors ? "Mentor" : "Student", success, total));
        response.put("summary", summary);
        response.put("errors", errors);
        return response;
    }

    private void importMentorRow(Map<String, String> row, List<String> errors, int rowNumber) {
        String email = require(row, "email").toLowerCase(Locale.ROOT);
        validateEmail(email);
        String username = valueOr(row.get("username"), email);
        ensureUnique(email, username, rowNumber);

        Mentor mentor = new Mentor();
        mentor.setEmail(email);
        mentor.setUsername(username);
        mentor.setPassword(passwordEncoder.encode(valueOr(row.get("password"), DEFAULT_PASSWORD)));
        mentor.setFirstName(require(row, "firstname"));
        mentor.setLastName(require(row, "lastname"));
        mentor.setPhoneNumber(valueOr(row.get("phonenumber"), null));
        mentor.setRole(User.UserRole.ROLE_MENTOR);
        mentor.setActive(true);
        mentor.setCreatedAt(LocalDateTime.now());
        mentor.setUpdatedAt(LocalDateTime.now());
        mentor.setMentorId(valueOr(row.get("mentorid"), generateMentorId()));
        mentor.setDepartment(valueOr(row.get("department"), null));
        mentor.setSpecialization(valueOr(row.get("specialization"), null));
        mentor.setDesignation(valueOr(row.get("designation"), null));
        mentor.setYearsOfExperience(parseInt(row.get("yearsofexperience"), 0));
        mentor.setMaxStudents(parseInt(row.get("maxstudents"), 10));

        if (mentorRepository.existsByMentorId(mentor.getMentorId())) {
            throw new IllegalArgumentException("mentorId '" + mentor.getMentorId() + "' already exists");
        }

        mentorRepository.save(mentor);
        log.info("Bulk-imported mentor {}", email);
    }

    private void importStudentRow(Map<String, String> row, List<String> errors, int rowNumber) {
        String email = require(row, "email").toLowerCase(Locale.ROOT);
        validateEmail(email);
        String username = valueOr(row.get("username"), email);
        ensureUnique(email, username, rowNumber);

        Student student = new Student();
        student.setEmail(email);
        student.setUsername(username);
        student.setPassword(passwordEncoder.encode(valueOr(row.get("password"), DEFAULT_PASSWORD)));
        student.setFirstName(require(row, "firstname"));
        student.setLastName(require(row, "lastname"));
        student.setPhoneNumber(valueOr(row.get("phonenumber"), null));
        student.setRole(User.UserRole.ROLE_STUDENT);
        student.setActive(true);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        student.setStudentId(valueOr(row.get("studentid"), generateStudentId()));
        student.setDepartment(valueOr(row.get("department"), null));
        student.setCourse(valueOr(row.get("course"), null));
        student.setBatch(valueOr(row.get("batch"), null));
        student.setSemester(parseInt(row.get("semester"), 0));
        student.setAttendance(parseDouble(row.get("attendance"), 0.0));
        student.setCgpa(parseDouble(row.get("cgpa"), 0.0));

        if (studentRepository.existsByStudentId(student.getStudentId())) {
            throw new IllegalArgumentException("studentId '" + student.getStudentId() + "' already exists");
        }

        studentRepository.save(student);
        log.info("Bulk-imported student {}", email);
    }

    private List<String> readLines(MultipartFile file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!lines.isEmpty() || !line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        if (!lines.isEmpty()) {
            lines.set(0, lines.get(0).replace("\uFEFF", ""));
        }
        return lines;
    }

    private char detectDelimiter(String headerLine) {
        long commas = headerLine.chars().filter(c -> c == ',').count();
        long semicolons = headerLine.chars().filter(c -> c == ';').count();
        return semicolons > commas ? ';' : ',';
    }

    private List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private Map<String, String> toRow(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), i < values.size() ? values.get(i) : "");
        }
        return row;
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private String require(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("'" + key + "' is required");
        }
        return value.trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void validateEmail(String email) {
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("invalid email '" + email + "'");
        }
    }

    private void ensureUnique(String email, String username, int rowNumber) {
        boolean emailTaken = studentRepository.existsByEmail(email)
                || mentorRepository.existsByEmail(email)
                || adminRepository.existsByEmail(email);
        if (emailTaken) {
            throw new IllegalArgumentException("email '" + email + "' already exists");
        }
        boolean usernameTaken = studentRepository.existsByUsername(username)
                || mentorRepository.existsByUsername(username)
                || adminRepository.existsByUsername(username);
        if (usernameTaken) {
            throw new IllegalArgumentException("username '" + username + "' already exists");
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : (int) Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String generateMentorId() {
        String id;
        do {
            id = "MEN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        } while (mentorRepository.existsByMentorId(id));
        return id;
    }

    private String generateStudentId() {
        String id;
        do {
            id = "STU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        } while (studentRepository.existsByStudentId(id));
        return id;
    }
}
