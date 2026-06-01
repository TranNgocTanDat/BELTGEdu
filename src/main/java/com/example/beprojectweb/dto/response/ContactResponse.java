package com.example.beprojectweb.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String organization;
    private String subject;
    private String message;
    private String createdAt;
    private boolean isRead;
}