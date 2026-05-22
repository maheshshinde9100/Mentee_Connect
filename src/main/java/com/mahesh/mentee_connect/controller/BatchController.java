package com.mahesh.mentee_connect.controller;

import org.springframework.data.domain.Page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mahesh.mentee_connect.model.Batch;
import com.mahesh.mentee_connect.model.Student;
import com.mahesh.mentee_connect.model.Response;
import com.mahesh.mentee_connect.model.ErrorResponse;
import com.mahesh.mentee_connect.payload.request.BatchMentorAssignRequest;
import com.mahesh.mentee_connect.payload.request.BatchStudentsAssignRequest;
import com.mahesh.mentee_connect.service.BatchService;
import com.mahesh.mentee_connect.dto.BatchAssignmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import com.mahesh.mentee_connect.model.Mentor;
import com.mahesh.mentee_connect.payload.request.BatchMentorsAssignRequest;

@RestController
@RequestMapping("/admin/batches")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Batches", description = "Batch management APIs")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @PostMapping
    public ResponseEntity<Batch> createBatch(@RequestBody Batch batch) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(batchService.createBatch(batch));
    }

    @GetMapping
    public ResponseEntity<Page<Batch>> getAllBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(batchService.getAllBatches(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get batch by ID", description = "Retrieve details of a specific batch by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved batch"),
        @ApiResponse(responseCode = "404", description = "Batch not found")
    })
    public ResponseEntity<Batch> getBatchById(@PathVariable String id) {
        Batch batch = batchService.getBatchById(id);
        return ResponseEntity.ok(batch);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update batch information", description = "Update details of a specific batch by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated batch"),
        @ApiResponse(responseCode = "404", description = "Batch not found"),
        @ApiResponse(responseCode = "400", description = "Invalid batch data provided")
    })
    public ResponseEntity<Batch> updateBatch(
            @PathVariable String id,
            @Valid @RequestBody Batch batch) {
        return ResponseEntity.ok(batchService.updateBatch(id, batch));
    }

    @PostMapping("/{id}/assign-mentor")
    public ResponseEntity<Batch> assignMentorToBatch(
            @PathVariable String id,
            @RequestBody BatchMentorAssignRequest request) {
        return ResponseEntity.ok(
                batchService.assignMentorToBatch(id, request.getMentorId()));
    }

    @PostMapping("/assign-mentor")
    @Operation(summary = "Assign multiple mentors to a batch", 
              description = "Assign multiple mentors to a specific batch")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully assigned mentors to batch"),
        @ApiResponse(responseCode = "404", description = "Batch or Mentor not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Batch> assignMentorsToBatch(
            @Valid @RequestBody BatchMentorsAssignRequest request) {
        return ResponseEntity.ok(
                batchService.assignMentorsToBatch(request.getBatchId(), request.getMentorIds()));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<List<Student>> getBatchStudents(@PathVariable String id) {
        return ResponseEntity.ok(batchService.getBatchStudents(id));
    }

    @GetMapping("/{id}/mentors")
    @Operation(summary = "Get mentors for a batch", description = "Retrieve all mentors assigned to a specific batch")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved mentors"),
        @ApiResponse(responseCode = "404", description = "Batch not found")
    })
    public ResponseEntity<List<Mentor>> getBatchMentors(@PathVariable String id) {
        return ResponseEntity.ok(batchService.getBatchMentors(id));
    }

    @PostMapping("/assign-students")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign multiple students to a batch", 
               description = "Assign a list of students to a specific batch")
    public ResponseEntity<?> assignStudentsToBatchBulk(
            @Valid @RequestBody BatchAssignmentRequest request) {
        try {
            List<Student> updatedStudents = batchService.assignStudentsToBatch(
                request.getBatchId(), 
                request.getStudentIds()
            );
            return ResponseEntity.ok(new Response(
                "Successfully assigned " + updatedStudents.size() + " students to batch", 
                true
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign-students")
    @Operation(summary = "Assign students to batch", description = "Assign multiple students to a batch (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Students successfully assigned to batch"),
        @ApiResponse(responseCode = "404", description = "Batch or Student not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<List<Student>> assignStudentsToBatch(
            @PathVariable String id,
            @Valid @RequestBody BatchStudentsAssignRequest request) {
        return ResponseEntity.ok(
                batchService.assignStudentsToBatch(id, request.getStudentIds()));
    }
}