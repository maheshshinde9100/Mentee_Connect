package com.mahesh.mentee_connect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentSkill {
    private String id;
    private String name;
    private String proficiency;
    private String description;
}
