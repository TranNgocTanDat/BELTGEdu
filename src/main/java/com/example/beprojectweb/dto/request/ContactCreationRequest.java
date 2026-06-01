package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactCreationRequest {
    @NotBlank(message = "NAME_REQUIRED")
    @Size(min = 2, max = 100, message = "NAME_INVALID")
    private String name;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    private String email;

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "PHONE_INVALID")
    private String phone;

    @NotBlank(message = "ORGANIZATION_REQUIRED")
    @Size(min = 2, max = 200, message = "ORGANIZATION_INVALID")
    private String organization;

    @NotBlank(message = "SUBJECT_REQUIRED")
    @Size(min = 3, max = 200, message = "SUBJECT_INVALID")
    private String subject;

    @NotBlank(message = "MESSAGE_REQUIRED")
    @Size(min = 10, max = 2000, message = "MESSAGE_INVALID")
    private String message;
}