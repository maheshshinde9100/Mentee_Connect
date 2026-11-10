// RegisterRequest.java
package com.mahesh.mentee_connect.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    /** STUDENT (a.k.a. MENTEE) or MENTOR */
    @NotBlank
    private String role;
}
