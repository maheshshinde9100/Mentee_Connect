package com.mahesh.mentee_connect.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.model.Response;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.payload.request.MentorAssignRequest;
import com.mahesh.mentee_connect.service.MentorService;
import com.mahesh.mentee_connect.service.StudentService;
import com.mahesh.mentee_connect.dto.StudentDTO;
import com.mahesh.mentee_connect.dto.MentorDTO;
import com.mahesh.mentee_connect.dto.MentorConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired
    private StudentService studentService;
    
    @Autowired
    private MentorService mentorService;
    
    @Autowired
    private MentorConverter mentorConverter;

    // Student Management
    @PostMapping("/students")
    public ResponseEntity<Student> createStudent(@RequestBody Student student) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.createStudent(student));
    }

    @Operation(summary = "Get all students", description = "Retrieve a list of all students")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires admin role")
    })
    @GetMapping("/students")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<StudentDTO>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(studentService.getAllStudentsAsDTOPageable(page, size));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable String id, 
            @RequestBody Student student) {
        return ResponseEntity.ok(studentService.updateStudent(id, student));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable String id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    // Mentor Management
    @PostMapping("/mentors")
    public ResponseEntity<Mentor> createMentor(@RequestBody Mentor mentor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mentorService.createMentor(mentor));
    }

    @GetMapping("/mentors")
    public ResponseEntity<?> getAllMentors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Mentor> mentorsPage = mentorService.getAllMentors(page, size);
        Page<MentorDTO> mentorDTOs = mentorsPage.map(mentor -> mentorConverter.convertToDTO(mentor));
        return ResponseEntity.ok(mentorDTOs);
    }

    @GetMapping("/mentors/{id}")
    public ResponseEntity<?> getMentorById(@PathVariable String id) {
        Mentor mentor = mentorService.getMentorById(id);
        MentorDTO dto = mentorConverter.convertToDTO(mentor);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/assign-mentor")
    public ResponseEntity<?> assignMentor(@Valid @RequestBody MentorAssignRequest request) {
        List<Student> updatedStudents = studentService.assignMentorToMultipleStudents(
            request.getStudentIds(), 
            request.getMentorId()
        );
        return ResponseEntity.ok(new Response("Successfully assigned " + updatedStudents.size() + " students to mentor", true));
    }
}