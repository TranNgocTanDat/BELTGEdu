package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.RegionRequest;
import com.example.beprojectweb.dto.response.RegionResponse;
import com.example.beprojectweb.entity.Region;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.repository.RegionRepository;
import com.example.beprojectweb.repository.SchoolRepository;
import com.example.beprojectweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> getRegions() {
        User user = currentUser();
        if (user.getRoles() != null && (user.getRoles().contains(Role.ADMIN.name()) || user.getRoles().contains(Role.ADMIN_SCHOOL.name()))) {
            return regionRepository.findAll().stream().map(this::toResponse).toList();
        }
        return user.getRegion() == null ? List.of() : List.of(toResponse(user.getRegion()));
    }

    @Transactional
    public RegionResponse create(RegionRequest request) {
        String name = normalize(request.getName());
        if (regionRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name already exists");
        }
        return toResponse(regionRepository.save(Region.builder().name(name).build()));
    }

    @Transactional
    public RegionResponse rename(UUID id, RegionRequest request) {
        Region region = findRegion(id);
        String name = normalize(request.getName());
        if (regionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name already exists");
        }
        region.setName(name);
        return toResponse(regionRepository.save(region));
    }

    @Transactional
    public void delete(UUID id) {
        Region region = findRegion(id);
        if (userRepository.existsByRegion_Id(id) || schoolRepository.existsByRegion_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Move users and schools to another region before deleting this region");
        }
        regionRepository.delete(region);
    }

    private Region findRegion(UUID id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));
    }

    private String normalize(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Region name is required");
        return normalized;
    }

    private RegionResponse toResponse(Region region) {
        return new RegionResponse(region.getId(), region.getName());
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new com.example.beprojectweb.exception.AppException(
                        com.example.beprojectweb.exception.ErrorCode.USER_NOT_EXISTS));
    }
}
