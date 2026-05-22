// Mentor.java
package com.mahesh.mentee_connect.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Document(collection = "mentors")
public class Mentor extends User {
    @Indexed(unique = true)
    private String mentorId;
    @Indexed
    private String department;
    private String specialization;
    private String designation;
    private int yearsOfExperience;
    
    @DBRef(lazy = true)
    private List<Student> assignedStudents = new ArrayList<>();
    
    private int maxStudents = 10;
    
    public Mentor(String username, String email, String password, String firstName, 
                 String lastName, String phoneNumber, String mentorId, 
                 String department, String specialization, String designation, 
                 int yearsOfExperience) {
        super();
        this.setUsername(username);
        this.setEmail(email);
        this.setPassword(password);
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setPhoneNumber(phoneNumber);
        this.setRole(UserRole.ROLE_MENTOR);
        this.mentorId = mentorId;
        this.department = department;
        this.specialization = specialization;
        this.designation = designation;
        this.yearsOfExperience = yearsOfExperience;
    }
}