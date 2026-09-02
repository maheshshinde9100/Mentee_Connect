// Student.java
package com.mahesh.mentee_connect.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Document(collection = "students")
public class Student extends User {
    @Indexed(unique = true)
    private String studentId;
    private String course;
    @Indexed
    private String batch;
    private int semester;
    private String department;
    private LocalDate dob;
    private String profilePhoto;
    
    @DBRef(lazy = true)
    private Mentor assignedMentor;
    
    private double attendance;
    private double cgpa;
    
    public Student(String username, String email, String password, String firstName, 
                  String lastName, String phoneNumber, String studentId, 
                  String course, String batch, int semester, String department,
                  LocalDate dob, String profilePhoto) {
        super();
        this.setUsername(username);
        this.setEmail(email);
        this.setPassword(password);
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setPhoneNumber(phoneNumber);
        this.setRole(UserRole.ROLE_STUDENT);
        this.studentId = studentId;
        this.course = course;
        this.batch = batch;
        this.semester = semester;
        this.department = department;
        this.dob = dob;
        this.profilePhoto = profilePhoto;
        this.attendance = 0.0;
        this.cgpa = 0.0;
    }
    
    // Getters and setters for new fields
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public LocalDate getDob() {
        return dob;
    }
    
    public void setDob(LocalDate dob) {
        this.dob = dob;
    }
    
    public String getProfilePhoto() {
        return profilePhoto;
    }
    
    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }
}