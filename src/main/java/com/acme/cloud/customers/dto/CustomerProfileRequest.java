package com.acme.cloud.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerProfileRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email @Size(max = 320) String email
) {
    public CustomerProfileRequest {
        if (fullName != null) {
            fullName = fullName.strip();
        }
        if (email != null) {
            email = email.strip();
        }
    }
}
