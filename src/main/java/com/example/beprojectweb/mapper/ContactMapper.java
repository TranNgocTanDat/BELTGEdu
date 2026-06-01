package com.example.beprojectweb.mapper;

import com.example.beprojectweb.dto.request.ContactCreationRequest;
import com.example.beprojectweb.dto.response.ContactResponse;
import com.example.beprojectweb.entity.Contact;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ContactMapper {
    ContactMapper INSTANCE = Mappers.getMapper(ContactMapper.class);

    Contact toContact(ContactCreationRequest request);
    ContactResponse toContactResponse(Contact contact);
    void updateContact(@MappingTarget Contact contact, ContactCreationRequest request);
}