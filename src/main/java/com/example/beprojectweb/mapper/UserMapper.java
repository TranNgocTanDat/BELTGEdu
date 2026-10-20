package com.example.beprojectweb.mapper;

import com.example.beprojectweb.dto.request.UserCreationRequest;
import com.example.beprojectweb.dto.request.UserUpdateRequest;
import com.example.beprojectweb.dto.response.UserResponse;
import com.example.beprojectweb.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request); //

    @Mapping(target = "regionId", source = "region.id")
    @Mapping(target = "regionName", source = "region.name")
    UserResponse toUserResponse(User user);
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
