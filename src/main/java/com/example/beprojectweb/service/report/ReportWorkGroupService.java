package com.example.beprojectweb.service.report;

import com.example.beprojectweb.dto.request.report.ReportWorkGroupRequest;
import com.example.beprojectweb.dto.response.report.ReportWorkGroupResponse;
import com.example.beprojectweb.entity.report.ReportWorkGroup;
import com.example.beprojectweb.repository.report.ReportWorkGroupRepository;
import com.example.beprojectweb.repository.report.ReportWorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REPORT_WORK')")
public class ReportWorkGroupService {
    private static final List<String> DEFAULT_GROUPS = List.of("Vận hành trường", "Hỗ trợ giáo viên", "Chương trình/R&D", "PPCT", "Học liệu/LMS", "Công cụ & cải tiến");
    private final ReportWorkGroupRepository groups;
    private final ReportWorkRepository work;

    @Transactional
    public List<ReportWorkGroupResponse> getAll() {
        if (groups.count() == 0) DEFAULT_GROUPS.forEach(name -> { ReportWorkGroup group = new ReportWorkGroup(); group.setName(name); group.setActive(true); groups.save(group); });
        return groups.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional
    public ReportWorkGroupResponse create(ReportWorkGroupRequest request) {
        String name = normalize(request.getName());
        if (groups.existsByNameIgnoreCase(name)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhóm công việc đã tồn tại");
        ReportWorkGroup group = new ReportWorkGroup();
        group.setName(name);
        group.setActive(request.getActive() == null || request.getActive());
        return response(groups.save(group));
    }

    @Transactional
    public ReportWorkGroupResponse update(UUID id, ReportWorkGroupRequest request) {
        ReportWorkGroup group = groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm công việc"));
        String name = normalize(request.getName());
        if (groups.existsByNameIgnoreCaseAndIdNot(name, id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhóm công việc đã tồn tại");
        group.setName(name);
        if (request.getActive() != null) group.setActive(request.getActive());
        return response(group);
    }

    @Transactional
    public void delete(UUID id) {
        ReportWorkGroup group = groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm công việc"));
        if (work.existsByWorkGroup_Id(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhóm đang được sử dụng. Hãy ngừng kích hoạt thay vì xóa.");
        groups.delete(group);
    }

    private String normalize(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên nhóm là bắt buộc");
        return name;
    }
    private ReportWorkGroupResponse response(ReportWorkGroup group) { return new ReportWorkGroupResponse(group.getId(), group.getName(), group.isActive()); }
}
