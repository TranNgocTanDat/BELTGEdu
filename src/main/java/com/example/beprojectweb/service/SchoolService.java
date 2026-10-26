package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.SchoolRequest;
import com.example.beprojectweb.dto.response.SchoolResponse;
import com.example.beprojectweb.entity.Region;
import com.example.beprojectweb.entity.School;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.repository.RegionRepository;
import com.example.beprojectweb.repository.RegistrationRepository;
import com.example.beprojectweb.repository.AnnouncementRepository;
import com.example.beprojectweb.repository.SchoolRepository;
import com.example.beprojectweb.repository.SchoolClosureRepository;
import com.example.beprojectweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SchoolService {
    private final SchoolRepository schoolRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final SchoolClosureRepository closureRepository;
    private final RegistrationRepository registrationRepository;
    private final JdbcTemplate jdbcTemplate;

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional(readOnly = true)
    public List<SchoolResponse> getSchools() {
        User user = currentUser();
        List<School> schools = isAdmin(user)
                ? schoolRepository.findAllByOrderByCodeAsc()
                : user.getRegion() == null ? List.of() : schoolRepository.findAllByRegion_IdOrderByCodeAsc(user.getRegion().getId());
        return schools.stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional(readOnly = true)
    public SchoolResponse getSchool(UUID id) {
        User user = currentUser();
        School school = findSchool(id);
        assertAccess(user, school);
        return toResponse(school);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public SchoolResponse create(SchoolRequest request) {
        User user = currentUser();
        Region region = resolveRegionForCreate(user, request.getRegionId());
        String code = generateSchoolCode();
        School school = School.builder()
                .name(request.getName().trim())
                .code(code)
                .address(trimToNull(request.getAddress()))
                .contactName(trimToNull(request.getContactName()))
                .phone(trimToNull(request.getPhone()))
                .level(request.getLevel())
                .createdAt(LocalDate.now())
                .region(region)
                .build();
        return toResponse(schoolRepository.save(school));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public SchoolResponse update(UUID id, SchoolRequest request) {
        User user = currentUser();
        School school = findSchool(id);
        assertAccess(user, school);
        school.setName(request.getName().trim());
        school.setAddress(trimToNull(request.getAddress()));
        school.setContactName(trimToNull(request.getContactName()));
        school.setPhone(trimToNull(request.getPhone()));
        school.setLevel(request.getLevel());
        if (isAdmin(user)) {
            school.setRegion(findRegion(request.getRegionId()));
        }
        return toResponse(schoolRepository.save(school));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public void delete(UUID id) {
        User user = currentUser();
        School school = findSchool(id);
        assertAccess(user, school);
        if (!registrationRepository.findAllBySchool_Id(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete the school registrations before deleting this school");
        }
        announcementRepository.deleteAllBySchool_Id(id);
        closureRepository.deleteAllBySchool_Id(id);
        schoolRepository.delete(school);
    }

    private Region resolveRegionForCreate(User user, UUID requestedRegionId) {
        if (isAdmin(user)) return findRegion(requestedRegionId);
        if (user.getRegion() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assign this user to a region before creating schools");
        }
        if (requestedRegionId != null && !requestedRegionId.equals(user.getRegion().getId())) {
            throw new AccessDeniedException("You can only create schools in your assigned region");
        }
        return user.getRegion();
    }

    private void assertAccess(User user, School school) {
        if (isAdmin(user)) return;
        UUID userRegion = user.getRegion() == null ? null : user.getRegion().getId();
        if (userRegion == null || !userRegion.equals(school.getRegion().getId())) {
            throw new AccessDeniedException("You can only manage schools in your assigned region");
        }
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && (user.getRoles().contains(Role.ADMIN.name()) || user.getRoles().contains(Role.ADMIN_SCHOOL.name()));
    }

    private School findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    private Region findRegion(UUID id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A region is required");
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));
    }

    private String generateSchoolCode() {
        jdbcTemplate.update("UPDATE school_code_sequence SET current_value = LAST_INSERT_ID(current_value + 1) WHERE sequence_name = 'school'");
        Long next = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (next == null) throw new IllegalStateException("Could not allocate a school code");
        return String.format(Locale.ROOT, "TR-%03d", next);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private SchoolResponse toResponse(School school) {
        return SchoolResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .code(school.getCode())
                .address(school.getAddress())
                .contactName(school.getContactName())
                .phone(school.getPhone())
                .level(school.getLevel())
                .regionId(school.getRegion().getId())
                .regionName(school.getRegion().getName())
                .createdAt(school.getCreatedAt())
                .build();
    }
}
