package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.MenteeTask;
import com.mahesh.mentee_connect.service.MenteeTaskService;
import com.mahesh.mentee_connect.service.MentorService;
import com.mahesh.mentee_connect.security.services.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mentors")
@Tag(name = "Mentee Tasks", description = "Mentee task management APIs")
public class MenteeTaskController {

    @Autowired
    private MenteeTaskService menteeTaskService;

    @Autowired
    private MentorService mentorService;

    @Autowired
    private SecurityService securityService;

    @PostMapping("/{mentorId}/tasks")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Create task", description = "Create a new task for a mentee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<MenteeTask> createTask(@PathVariable String mentorId, @Valid @RequestBody MenteeTask task) {
        if (!securityService.isCurrentUser(mentorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        task.setMentorId(mentorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(menteeTaskService.createTask(task));
    }

    @GetMapping("/{mentorId}/tasks")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Get tasks", description = "Get all tasks created by a mentor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<List<MenteeTask>> getTasksByMentor(@PathVariable String mentorId) {
        if (!securityService.isCurrentUser(mentorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(menteeTaskService.getTasksByMentor(mentorId));
    }

    @GetMapping("/me/tasks")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Get current mentor's tasks", description = "Get all tasks created by the current logged-in mentor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<MenteeTask>> getMyTasks() {
        com.mahesh.mentee_connect.model.Mentor currentMentor = mentorService.getCurrentMentor();
        return ResponseEntity.ok(menteeTaskService.getTasksByMentor(currentMentor.getId()));
    }

    @GetMapping("/{mentorId}/students/{studentId}/tasks")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Get student tasks", description = "Get tasks for a specific student assigned by a mentor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<List<MenteeTask>> getTasksByMentorAndStudent(
            @PathVariable String mentorId, @PathVariable String studentId) {
        if (!securityService.isCurrentUser(mentorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(menteeTaskService.getTasksByMentorAndStudent(mentorId, studentId));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Update task", description = "Update an existing task")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<MenteeTask> updateTask(
            @PathVariable String taskId, @Valid @RequestBody MenteeTask task) {
        if (!securityService.isTaskOwner(taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(menteeTaskService.updateTask(taskId, task));
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('MENTOR')")
    @Operation(summary = "Delete task", description = "Delete an existing task")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        if (!securityService.isTaskOwner(taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        menteeTaskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
} 