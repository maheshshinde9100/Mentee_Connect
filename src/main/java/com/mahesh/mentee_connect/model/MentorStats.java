package com.mahesh.mentee_connect.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.List;
import com.mahesh.mentee_connect.model.Student;
@Data
@AllArgsConstructor
public class MentorStats {
    private String mentorId;
    private String mentorName;
    private List<Student> students;
}