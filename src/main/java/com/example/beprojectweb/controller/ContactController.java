package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.ContactCreationRequest;
import com.example.beprojectweb.dto.response.ContactResponse;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.service.ContactService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContactController {
    ContactService contactService;

    @PostMapping
    public APIResponse<ContactResponse> createContact(@RequestBody @Valid ContactCreationRequest request) {
        APIResponse<ContactResponse> apiResponse = new APIResponse<>();
        apiResponse.setResult(contactService.createContact(request));
        return apiResponse;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<List<ContactResponse>> getAllContacts() {
        return APIResponse.<List<ContactResponse>>builder()
                .result(contactService.getAllContacts())
                .build();
    }

    @GetMapping("/{id}")
    public APIResponse<ContactResponse> getContactById(@PathVariable String id) {
        return APIResponse.<ContactResponse>builder()
                .result(contactService.getContactById(id))
                .build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> deleteContact(@PathVariable String id) {
        contactService.deleteContact(id);
        return APIResponse.<Void>builder().build();
    }

    @PatchMapping("/{id}/read")
    public APIResponse<Void> markAsRead(@PathVariable String id) {
        contactService.markAsRead(id);
        return APIResponse.<Void>builder().build();
    }
}