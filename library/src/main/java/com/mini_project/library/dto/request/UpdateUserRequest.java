package com.mini_project.library.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20)
    private String phoneNumber;
}
