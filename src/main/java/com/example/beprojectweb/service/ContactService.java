package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.ContactCreationRequest;
import com.example.beprojectweb.dto.response.ContactResponse;
import com.example.beprojectweb.entity.Contact;
import com.example.beprojectweb.mapper.ContactMapper;
import com.example.beprojectweb.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {
    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;

    public ContactResponse createContact(ContactCreationRequest request) {
        Contact contact = contactMapper.toContact(request);
        Contact savedContact = contactRepository.save(contact);
        return contactMapper.toContactResponse(savedContact);
    }

    public List<ContactResponse> getAllContacts() {
        return contactRepository.findAll().stream()
                .map(contactMapper::toContactResponse)
                .collect(Collectors.toList());
    }

    public ContactResponse getContactById(String id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        return contactMapper.toContactResponse(contact);
    }

    public void deleteContact(String id) {
        contactRepository.deleteById(id);
    }

    public void markAsRead(String id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        contact.setRead(true);
        contactRepository.save(contact);
    }
}