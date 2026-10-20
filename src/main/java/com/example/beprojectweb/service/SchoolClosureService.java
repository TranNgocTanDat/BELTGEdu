package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.SchoolClosureRequest;
import com.example.beprojectweb.dto.response.SchoolClosureResponse;
import com.example.beprojectweb.entity.School;
import com.example.beprojectweb.entity.SchoolClosure;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.repository.SchoolClosureRepository;
import com.example.beprojectweb.repository.SchoolRepository;
import com.example.beprojectweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolClosureService {
    private final SchoolClosureRepository closureRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional(readOnly = true)
    public List<SchoolClosureResponse> getClosures(UUID schoolId) {
        User user = currentUser();
        List<SchoolClosure> items;
        if (schoolId != null) {
            School school = findSchool(schoolId);
            assertSchoolAccess(user, school);
            items = closureRepository.findAllBySchool_IdOrderByWeekStartAsc(schoolId);
        } else if (isAdmin(user)) {
            items = closureRepository.findAllByOrderByWeekStartAsc();
        } else if (user.getRegion() != null) {
            items = closureRepository.findAllBySchool_Region_IdOrderByWeekStartAsc(user.getRegion().getId());
        } else {
            items = List.of();
        }
        return items.stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional
    public SchoolClosureResponse addOrUpdate(SchoolClosureRequest request) {
        User user = currentUser();
        School school = findSchool(request.getSchoolId());
        assertSchoolAccess(user, school);
        if (request.getWeekStart().getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weekStart must be a Monday");
        }
        SchoolClosure closure = closureRepository.findBySchool_IdAndWeekStart(school.getId(), request.getWeekStart())
                .orElseGet(SchoolClosure::new);
        closure.setSchool(school);
        closure.setWeekStart(request.getWeekStart());
        closure.setReason(request.getReason().trim());
        return toResponse(closureRepository.save(closure));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Transactional
    public void remove(UUID id) {
        User user = currentUser();
        SchoolClosure closure = closureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School closure not found"));
        assertSchoolAccess(user, closure.getSchool());
        closureRepository.delete(closure);
    }

    private School findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    private void assertSchoolAccess(User user, School school) {
        if (isAdmin(user)) return;
        UUID userRegion = user.getRegion() == null ? null : user.getRegion().getId();
        if (userRegion == null || !userRegion.equals(school.getRegion().getId())) {
            throw new AccessDeniedException("You can only manage closures for schools in your region");
        }
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().contains(Role.ADMIN.name());
    }

    private SchoolClosureResponse toResponse(SchoolClosure item) {
        return SchoolClosureResponse.builder()
                .id(item.getId())
                .schoolId(item.getSchool().getId())
                .weekStart(item.getWeekStart())
                .reason(item.getReason())
                .build();
    }
}
