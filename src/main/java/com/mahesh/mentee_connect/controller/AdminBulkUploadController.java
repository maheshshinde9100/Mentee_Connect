package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.service.BulkUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBulkUploadController {

    @Autowired
    private BulkUploadService bulkUploadService;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/mentors/bulk-upload")
    public ResponseEntity<Map<String, Object>> uploadMentors(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, true);
    }

    @PostMapping("/students/bulk-upload")
    public ResponseEntity<Map<String, Object>> uploadStudents(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, false);
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getDepartments() {
        Map<String, int[]> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Mentor mentor : mentorRepository.findAll()) {
            countDepartment(counts, mentor.getDepartment(), 0);
        }
        for (Student student : studentRepository.findAll()) {
            countDepartment(counts, student.getDepartment(), 1);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        counts.forEach((name, value) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("mentorCount", value[0]);
            entry.put("studentCount", value[1]);
            entry.put("total", value[0] + value[1]);
            result.add(entry);
        });
        return ResponseEntity.ok(result);
    }

    private void countDepartment(Map<String, int[]> counts, String department, int index) {
        if (department == null || department.isBlank()) {
            return;
        }
        String key = department.trim();
        counts.computeIfAbsent(key, k -> new int[2])[index]++;
    }

    private ResponseEntity<Map<String, Object>> handleUpload(MultipartFile file, boolean mentors) {
        if (file == null || file.isEmpty()) {
            return badRequest("Please choose a CSV file to upload");
        }

        String filename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv")) {
            return badRequest("Only CSV files are supported. Export your spreadsheet as .csv and try again.");
        }

        try {
            Map<String, Object> response = mentors
                    ? bulkUploadService.uploadMentors(file)
                    : bulkUploadService.uploadStudents(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Bulk upload failed: " + e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
