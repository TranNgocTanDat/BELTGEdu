package com.example.beprojectweb.service.report;

import com.example.beprojectweb.entity.report.ReportGoal;
import com.example.beprojectweb.entity.report.ReportIssue;
import com.example.beprojectweb.entity.report.ReportKpi;
import com.example.beprojectweb.entity.report.ReportStaff;
import com.example.beprojectweb.entity.report.ReportWork;
import com.example.beprojectweb.entity.report.ReportWorkUpdate;
import com.example.beprojectweb.entity.report.ReportWorkExtension;
import com.example.beprojectweb.repository.report.ReportGoalRepository;
import com.example.beprojectweb.repository.report.ReportIssueRepository;
import com.example.beprojectweb.repository.report.ReportKpiRepository;
import com.example.beprojectweb.repository.report.ReportStaffRepository;
import com.example.beprojectweb.repository.report.ReportWorkRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REPORT_WORK')")
public class ReportExcelService {
    private static final Map<String, String> KPI_LABELS = Map.of(
            "schools", "Số trường được hỗ trợ",
            "teachers", "Số giáo viên được hỗ trợ",
            "programs", "Số chương trình đã rà soát",
            "ppct", "Số PPCT hoàn thành",
            "lms", "Số gói chương trình LMS",
            "issues", "Số vấn đề đã xử lý"
    );

    private final ReportStaffRepository staffRepository;
    private final ReportWorkRepository workRepository;
    private final ReportKpiRepository kpiRepository;
    private final ReportGoalRepository goalRepository;
    private final ReportIssueRepository issueRepository;

    @Transactional(readOnly = true)
    public byte[] export(String period, LocalDate startDate, LocalDate endDate, LocalDate cutoffDate) {
        boolean dateRange = startDate != null || endDate != null;
        if (dateRange && (startDate == null || endDate == null || startDate.isAfter(endDate))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are both required and must form a valid range");
        }
        if (!dateRange && (period == null || !period.matches("\\d{4}-(0[1-9]|1[0-2]|Q[1-4]|W(0[1-9]|[1-4][0-9]|5[0-3]))|\\d{4}"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period must be YYYY, YYYY-MM, YYYY-Q1..Q4, or YYYY-W01..W53");
        }
        if (cutoffDate != null && (!dateRange || cutoffDate.isAfter(endDate))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cutoffDate must be supplied with a date range and cannot be after endDate");
        }
        String reportPeriod = period == null ? startDate.toString().substring(0, 7) : period;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate rangeStart = dateRange ? startDate : periodStart(reportPeriod);
        LocalDate rangeEnd = dateRange ? endDate : periodEnd(reportPeriod);
        if (cutoffDate != null && cutoffDate.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cutoffDate cannot be after today in Asia/Ho_Chi_Minh");
        }
        LocalDate expectedCutoff = rangeEnd.isBefore(today) ? rangeEnd : today;
        if (cutoffDate != null && !cutoffDate.equals(expectedCutoff)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cutoffDate must match the selected period cutoff");
        }
        LocalDate asOf = expectedCutoff;
        boolean futurePeriod = rangeStart.isAfter(today);
        boolean closedPeriod = !futurePeriod && rangeEnd.isBefore(today);

        List<ReportStaff> staff = staffRepository.findAllByOrderByNameAsc();
        List<ReportWork> allWork = workRepository.findAllByOrderByPeriodDescTitleAsc();
        List<ReportWork> work = allWork.stream()
                .filter(row -> dateRange ? overlaps(row, startDate, endDate) : inPeriod(row.getPeriod(), reportPeriod))
                .filter(row -> (dateRange && row.getStartDate() != null) || hasAdminDeadlineCorrection(row) || isCompletedBy(row, asOf) || row.getCreatedAt() == null || !row.getCreatedAt().toLocalDate().isAfter(asOf))
                .toList();
        List<ReportKpi> kpis = kpiRepository.findAllByOrderByPeriodDescKpiKeyAsc().stream()
                .filter(row -> dateRange ? periodTouchesRange(row.getPeriod(), startDate, endDate) : inPeriod(row.getPeriod(), reportPeriod)).toList();
        List<ReportGoal> goals = goalRepository.findAllByOrderByPeriodDescTitleAsc().stream()
                .filter(row -> dateRange ? periodTouchesRange(row.getPeriod(), startDate, endDate) : inPeriod(row.getPeriod(), reportPeriod)).toList();
        List<ReportIssue> issues = futurePeriod ? List.of() : issueRepository.findAllByOrderByLevelAscTitleAsc().stream()
                .filter(issue -> !dateRange || (issue.getPeriod() == null || issue.getPeriod().isBlank() || periodTouchesRange(issue.getPeriod(), startDate, asOf))
                        || (issue.getDueDate() != null && !issue.getDueDate().isBefore(startDate) && !issue.getDueDate().isAfter(asOf)))
                .toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle titleStyle = titleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);
            String label = dateRange ? dateRangeLabel(startDate, endDate) : periodLabel(reportPeriod);
            List<? extends List<?>> workRows = work.stream().map(row -> workExportRow(row, asOf, futurePeriod, closedPeriod, rangeStart)).toList();
            long completedWork = workRows.stream().filter(row -> "Hoàn thành".equals(row.get(8))).count();
            long openWork = workRows.stream().filter(row -> !"Hoàn thành".equals(row.get(8))).count();
            long overdueWork = workRows.stream().filter(row -> "Trễ hạn".equals(row.get(8))).count();
            long newOverdueWork = 0;
            long carryoverOverdueWork = 0;
            long onTimeOriginal = 0;
            long onTimeExtended = 0;
            long onTimeShortened = 0;
            long onTimeAdjusted = 0;
            long completedLate = workRows.stream().filter(row -> "Hoàn thành trễ".equals(row.get(9))).count();
            long newCompletedWork = 0;
            long carryoverCompletedWork = 0;
            long carryoverLateCompletedWork = 0;
            long newCompletedLateWork = 0;
            for (int index = 0; index < work.size(); index++) {
                List<?> exportRow = workRows.get(index);
                ReportWork source = work.get(index);
                boolean carryover = source.getStartDate() != null && source.getStartDate().isBefore(rangeStart);
                String status = String.valueOf(exportRow.get(8));
                if ("Trễ hạn".equals(status)) {
                    if (carryover) carryoverOverdueWork++; else newOverdueWork++;
                }
                if (!isDateWithinRange(exportRow.get(6), rangeStart, asOf)) continue;
                String deadlineResult = String.valueOf(exportRow.get(9));
                if (!carryover) {
                    newCompletedWork++;
                    if ("Đúng hạn ban đầu".equals(deadlineResult)) onTimeOriginal++;
                    if ("Đúng hạn sau gia hạn".equals(deadlineResult)) onTimeExtended++;
                    if ("Đúng hạn theo hạn rút ngắn".equals(deadlineResult)) onTimeShortened++;
                    if ("Đúng hạn sau điều chỉnh".equals(deadlineResult)) onTimeAdjusted++;
                    if ("Hoàn thành trễ".equals(deadlineResult)) newCompletedLateWork++;
                } else {
                    carryoverCompletedWork++;
                    if ("Hoàn thành trễ".equals(deadlineResult)) carryoverLateCompletedWork++;
                }
            }
            long extensionCount = work.stream().flatMap(row -> row.getExtensions().stream())
                    .filter(extension -> inRange(extension.getCreatedAt(), rangeStart, asOf))
                    .filter(extension -> extension.getChangeType() == null || "EXTENSION".equals(extension.getChangeType())).count();
            long shorteningCount = work.stream().flatMap(row -> row.getExtensions().stream())
                    .filter(extension -> inRange(extension.getCreatedAt(), rangeStart, asOf))
                    .filter(extension -> "SHORTENING".equals(extension.getChangeType())).count();
            long resetCount = work.stream().flatMap(row -> row.getExtensions().stream())
                    .filter(extension -> inRange(extension.getCreatedAt(), rangeStart, asOf))
                    .filter(extension -> "RESET".equals(extension.getChangeType())).count();
            long adjustmentCount = work.stream().flatMap(row -> row.getExtensions().stream())
                    .filter(extension -> "ADMIN_CORRECTION".equals(extension.getChangeType())).count();
            writeSheet(workbook, "Tổng quan", "BÁO CÁO QUẢN TRỊ · " + label,
                    List.of("Chỉ tiêu", "Kết quả"), List.of(
                            List.of("Nhân sự đang làm", staff.stream().filter(row -> "Đang làm".equals(row.getStatus())).count()),
                            List.of("Tổng số nhân sự", staff.size()),
                            List.of(futurePeriod ? "Công việc dự kiến" : "Đầu việc trong kỳ", work.size()),
                            List.of(futurePeriod ? "Mốc lập kế hoạch" : "Mốc chốt dữ liệu", asOf.toString()),
                            List.of(futurePeriod ? "Kết quả thực hiện" : "Tổng việc hoàn thành (gồm tồn kỳ trước)", futurePeriod ? "Chưa đến kỳ" : completedWork),
                            List.of("Việc phát sinh trong kỳ hoàn thành", futurePeriod ? "Chưa đến kỳ" : newCompletedWork),
                            List.of("Tồn từ kỳ trước hoàn thành", futurePeriod ? "Chưa đến kỳ" : carryoverCompletedWork),
                            List.of("Tồn kỳ trước hoàn thành trễ", futurePeriod ? "Chưa đến kỳ" : carryoverLateCompletedWork),
                            List.of("Công việc còn mở tại ngày chốt", futurePeriod ? "Chưa đến kỳ" : openWork),
                            List.of("Việc mới trễ hạn tại ngày chốt", futurePeriod ? "Chưa đến kỳ" : newOverdueWork),
                            List.of("Tồn kỳ trước vẫn trễ tại ngày chốt", futurePeriod ? "Chưa đến kỳ" : carryoverOverdueWork),
                            List.of("Tổng việc trễ hạn tại ngày chốt", futurePeriod ? "Chưa đến kỳ" : overdueWork),
                            List.of("Việc mới đúng hạn theo hạn ban đầu", futurePeriod ? "Chưa đến kỳ" : onTimeOriginal),
                            List.of("Việc mới đúng hạn sau gia hạn", futurePeriod ? "Chưa đến kỳ" : onTimeExtended),
                            List.of("Việc mới đúng hạn theo hạn rút ngắn", futurePeriod ? "Chưa đến kỳ" : onTimeShortened),
                            List.of("Việc mới đúng hạn sau điều chỉnh bởi ADMIN", futurePeriod ? "Chưa đến kỳ" : onTimeAdjusted),
                            List.of("Việc mới hoàn thành trễ", futurePeriod ? "Chưa đến kỳ" : newCompletedLateWork),
                            List.of("Tổng việc hoàn thành trễ (gồm tồn)", futurePeriod ? "Chưa đến kỳ" : completedLate),
                            List.of("Số lần gia hạn trong kỳ", futurePeriod ? "Chưa đến kỳ" : extensionCount),
                            List.of("Số lần rút ngắn hạn trong kỳ", futurePeriod ? "Chưa đến kỳ" : shorteningCount),
                            List.of("Số lần khôi phục hạn ban đầu trong kỳ", futurePeriod ? "Chưa đến kỳ" : resetCount),
                            List.of("Số lần điều chỉnh hạn ADMIN (kể cả hậu kỳ)", futurePeriod ? "Chưa đến kỳ" : adjustmentCount),
                            List.of(futurePeriod ? "Chỉ tiêu dự kiến" : "KPI được ghi nhận", kpis.size()),
                            List.of(futurePeriod ? "Mục tiêu dự kiến" : "Mục tiêu trong kỳ", goals.size()),
                            List.of("Tiến độ mục tiêu trung bình", futurePeriod ? "Chưa đến kỳ" : goals.isEmpty() ? "—" : average(goals.stream().mapToInt(ReportGoal::getProgress).toArray()) + "%"),
                            List.of("Vướng mắc đang ghi nhận", issues.size())
                    ), titleStyle, headerStyle, bodyStyle);

            writeSheet(workbook, "Nhân sự", "DANH SÁCH NHÂN SỰ", List.of("Họ và tên", "Vai trò", "Phạm vi phụ trách", "Đầu ra kỳ vọng", "Trạng thái"),
                    staff.stream().map(row -> List.of(row.getName(), row.getRole(), row.getScope(), row.getOutput(), row.getStatus())).toList(), titleStyle, headerStyle, bodyStyle);
            writeSheet(workbook, "Công việc", "CÔNG VIỆC · " + label, List.of("Nhóm", "Đầu việc", "Người phụ trách", "Ngày bắt đầu", "Hạn ban đầu", "Hạn hiện tại", "Ngày hoàn thành", "Tiến độ cuối kỳ", "Trạng thái cuối kỳ", "Đánh giá hạn", "Ghi chú", "Nguồn công việc", "Tuần hạn"),
                    workRows, titleStyle, headerStyle, bodyStyle);
            if (dateRange && !futurePeriod) {
                List<? extends List<?>> progressHistory = work.stream().flatMap(row -> row.getUpdates().stream()
                        .filter(update -> inRange(update.getUpdatedAt(), startDate, asOf))
                        .map(update -> List.of(update.getUpdatedAt().toString(), row.getGroup(), row.getTitle(), row.getOwner().getName(), update.getProgress() + "%", update.getStatus(), update.getNote(), update.getUpdatedBy())))
                        .map(row -> (List<?>) row).toList();
                writeSheet(workbook, "Cập nhật", "LỊCH SỬ CẬP NHẬT · " + label, List.of("Thời điểm", "Nhóm", "Công việc", "Phụ trách", "Tiến độ", "Trạng thái", "Ghi chú", "Người cập nhật"), progressHistory, titleStyle, headerStyle, bodyStyle);
                List<? extends List<?>> extensions = work.stream().flatMap(row -> row.getExtensions().stream()
                        .filter(extension -> "ADMIN_CORRECTION".equals(extension.getChangeType()) || inRange(extension.getCreatedAt(), startDate, asOf))
                        .map(extension -> List.of(extension.getCreatedAt().toString(), "ADMIN_CORRECTION".equals(extension.getChangeType()) ? "Điều chỉnh bởi ADMIN" : "SHORTENING".equals(extension.getChangeType()) ? "Rút ngắn hạn" : "RESET".equals(extension.getChangeType()) ? "Khôi phục hạn ban đầu" : "Gia hạn", row.getGroup(), row.getTitle(), extension.getPreviousDueDate().toString(), extension.getNewDueDate().toString(), extension.getReason(), extension.getExtendedBy())))
                        .map(row -> (List<?>) row).toList();
                writeSheet(workbook, "Thay đổi hạn", "LỊCH SỬ THAY ĐỔI HẠN · " + label, List.of("Thời điểm", "Loại thay đổi", "Nhóm", "Công việc", "Hạn cũ", "Hạn mới", "Lý do", "Người thực hiện"), extensions, titleStyle, headerStyle, bodyStyle);
            }
            writeSheet(workbook, "KPI", (futurePeriod ? "CHỈ TIÊU DỰ KIẾN · " : "KẾT QUẢ KPI · ") + periodLabel(reportPeriod), List.of("Kỳ", "Chỉ số", "Mục tiêu", "Thực tế", "Mức đạt"),
                    kpis.stream().map(row -> futurePeriod
                            ? java.util.Arrays.asList(periodLabel(row.getPeriod()), KPI_LABELS.getOrDefault(row.getKpiKey(), row.getKpiKey()), row.getTarget(), "—", "Kế hoạch")
                            : java.util.Arrays.asList(periodLabel(row.getPeriod()), KPI_LABELS.getOrDefault(row.getKpiKey(), row.getKpiKey()), row.getTarget(), row.getActual(), row.getTarget() == 0 ? "—" : Math.round(row.getActual() * 100.0 / row.getTarget()) + "%")).toList(), titleStyle, headerStyle, bodyStyle);
            writeSheet(workbook, "Mục tiêu", "MỤC TIÊU · " + periodLabel(reportPeriod), List.of("Kỳ", "Mục tiêu", "Đầu ra cần đạt", "Phụ trách", "Tiến độ", "Trạng thái"),
                    goals.stream().map(row -> futurePeriod
                            ? java.util.Arrays.asList(periodLabel(row.getPeriod()), row.getTitle(), row.getOutput(), row.getOwner(), "—", "Kế hoạch")
                            : java.util.Arrays.asList(periodLabel(row.getPeriod()), row.getTitle(), row.getOutput(), row.getOwner(), row.getProgress() + "%", row.getStatus())).toList(), titleStyle, headerStyle, bodyStyle);
            writeSheet(workbook, "Vướng mắc", "VƯỚNG MẮC, RỦI RO VÀ HÀNH ĐỘNG · " + label, List.of("Loại", "Nội dung", "Mức độ", "Trạng thái", "Phụ trách", "Hạn xử lý", "Ảnh hưởng", "Hành động tiếp theo"),
                    issues.stream().map(row -> java.util.Arrays.asList(kindLabel(row.getKind()), row.getTitle(), row.getLevel(), statusLabel(row.getStatus()), row.getOwner(), dateText(row.getDueDate()), row.getImpact(), row.getProposal())).toList(), titleStyle, headerStyle, bodyStyle);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate report workbook", exception);
        }
    }

    private void writeSheet(Workbook workbook, String name, String title, List<String> headers, List<? extends List<?>> values,
                            CellStyle titleStyle, CellStyle headerStyle, CellStyle bodyStyle) {
        Sheet sheet = workbook.createSheet(name);
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        if (headers.size() > 1) sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.size() - 1));
        Row headerRow = sheet.createRow(2);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(headerStyle);
        }
        int rowIndex = 3;
        for (List<?> valuesRow : values) {
            Row row = sheet.createRow(rowIndex++);
            for (int column = 0; column < headers.size(); column++) {
                Cell cell = row.createCell(column);
                Object value = column < valuesRow.size() ? valuesRow.get(column) : "";
                if (value instanceof Number number) cell.setCellValue(number.doubleValue());
                else cell.setCellValue(value == null ? "" : value.toString());
                cell.setCellStyle(bodyStyle);
            }
        }
        if (!values.isEmpty()) sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, rowIndex - 1, 0, headers.size() - 1));
        sheet.createFreezePane(0, 3);
        for (int column = 0; column < headers.size(); column++) {
            int width = Math.min(60, Math.max(16, headers.get(column).length() + 4));
            sheet.setColumnWidth(column, width * 256);
        }
        sheet.setDefaultRowHeightInPoints(22);
    }

    private CellStyle titleStyle(Workbook workbook) {
        Font font = workbook.createFont(); font.setBold(true); font.setFontHeightInPoints((short) 15); font.setColor(IndexedColors.DARK_RED.getIndex());
        CellStyle style = workbook.createCellStyle(); style.setFont(font); style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle(); style.setFont(font); style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex()); style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN); style.setBottomBorderColor(IndexedColors.WHITE.getIndex()); style.setWrapText(true);
        return style;
    }

    private CellStyle bodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle(); style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP); style.setWrapText(true);
        style.setBorderBottom(BorderStyle.HAIR); style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private boolean inPeriod(String itemPeriod, String selectedPeriod) {
        if (selectedPeriod == null || itemPeriod == null) return false;
        LocalDate selectedStart = periodStart(selectedPeriod);
        LocalDate selectedEnd = periodEnd(selectedPeriod);
        LocalDate itemStart = periodStart(itemPeriod);
        LocalDate itemEnd = periodEnd(itemPeriod);
        return !itemStart.isBefore(selectedStart) && !itemEnd.isAfter(selectedEnd);
    }

    private boolean overlaps(ReportWork work, LocalDate start, LocalDate end) {
        if (work.getStartDate() == null) return periodTouchesRange(work.getPeriod(), start, end);
        if (work.getStartDate().isAfter(end)) return false;
        return work.getCompletedDate() == null || !work.getCompletedDate().isBefore(start);
    }

    private List<?> workExportRow(ReportWork work, LocalDate asOfDate, boolean planOnly, boolean closedPeriod, LocalDate rangeStart) {
        LocalDate dueDate = work.getOriginalDueDate();
        ReportWorkExtension latestExtension = work.getExtensions().stream()
                .filter(extension -> "ADMIN_CORRECTION".equals(extension.getChangeType()) || !extension.getCreatedAt().toLocalDate().isAfter(asOfDate))
                .max(java.util.Comparator.comparing(ReportWorkExtension::getCreatedAt)).orElse(null);
        if (latestExtension != null) dueDate = latestExtension.getNewDueDate();
        String sourceType = work.getStartDate() == null ? "Theo kỳ đã lưu" : work.getStartDate().isBefore(rangeStart) ? "Tồn từ kỳ trước" : "Phát sinh trong kỳ";
        if (planOnly) {
            return java.util.Arrays.asList(work.getWorkGroup() == null ? work.getGroup() : work.getWorkGroup().getName(), work.getTitle(), work.getOwner().getName(),
                    dateText(work.getStartDate()), dateText(work.getOriginalDueDate()), dateText(dueDate), "", "", "Kế hoạch", "Chưa đến kỳ", work.getImpact(), sourceType, dueWeekLabel(dueDate));
        }
        List<ReportWorkUpdate> history = work.getUpdates().stream()
                .filter(update -> !update.getUpdatedAt().toLocalDate().isAfter(asOfDate))
                .sorted(java.util.Comparator.comparing(ReportWorkUpdate::getUpdatedAt)).toList();
        ReportWorkUpdate last = history.isEmpty() ? null : history.get(history.size() - 1);
        String status = last == null ? work.getStatus() : last.getStatus();
        int progress = last == null ? work.getProgress() : last.getProgress();
        LocalDate completionEventDate = null;
        boolean wasComplete = false;
        for (ReportWorkUpdate update : history) {
            boolean isComplete = "Hoàn thành".equals(update.getStatus());
            if (isComplete && !wasComplete) completionEventDate = update.getUpdatedAt().toLocalDate();
            wasComplete = isComplete;
        }
        LocalDate recordedCompletionDate = work.getCompletedDate() != null && !work.getCompletedDate().isAfter(asOfDate)
                ? work.getCompletedDate() : null;
        LocalDate completedDate = last != null
                ? ("Hoàn thành".equals(last.getStatus()) ? (recordedCompletionDate != null ? recordedCompletionDate : completionEventDate) : null)
                : recordedCompletionDate;
        boolean completedByCutoff = completedDate != null;
        if (completedByCutoff) {
            status = "Hoàn thành";
            progress = 100;
        } else {
            if ("Trễ hạn".equals(status)) status = "Đang thực hiện";
            boolean completedAfterCutoff = last == null && work.getCompletedDate() != null && work.getCompletedDate().isAfter(asOfDate);
            if (completedAfterCutoff && "Hoàn thành".equals(status)) {
                status = "Đang thực hiện";
                progress = Math.min(progress, 99);
            }
            if (isOverdueAtCutoff(dueDate, asOfDate, closedPeriod)) status = "Trễ hạn";
        }
        String deadlineResult;
        if (completedByCutoff) {
            String changeType = latestExtension == null ? null : latestExtension.getChangeType();
            if (dueDate != null && completedDate.isAfter(dueDate)) deadlineResult = "Hoàn thành trễ";
            else if ("ADMIN_CORRECTION".equals(changeType) && work.getOriginalDueDate() != null && completedDate.isAfter(work.getOriginalDueDate())) deadlineResult = "Đúng hạn sau điều chỉnh";
            else if ("SHORTENING".equals(changeType)) deadlineResult = "Đúng hạn theo hạn rút ngắn";
            else if (("EXTENSION".equals(changeType) || changeType == null) && latestExtension != null && work.getOriginalDueDate() != null && completedDate.isAfter(work.getOriginalDueDate())) deadlineResult = "Đúng hạn sau gia hạn";
            else deadlineResult = "Đúng hạn ban đầu";
        } else {
            deadlineResult = isOverdueAtCutoff(dueDate, asOfDate, closedPeriod) ? "Quá hạn" : "Trong hạn";
        }
        return java.util.Arrays.asList(work.getWorkGroup() == null ? work.getGroup() : work.getWorkGroup().getName(), work.getTitle(), work.getOwner().getName(),
                dateText(work.getStartDate()), dateText(work.getOriginalDueDate()), dateText(dueDate), dateText(completedDate),
                progress + "%", status, deadlineResult, work.getImpact(), sourceType, dueWeekLabel(dueDate));
    }

    private boolean isOverdueAtCutoff(LocalDate dueDate, LocalDate cutoffDate, boolean closedPeriod) {
        return dueDate != null && (closedPeriod ? !dueDate.isAfter(cutoffDate) : dueDate.isBefore(cutoffDate));
    }

    private String dueWeekLabel(LocalDate dueDate) {
        if (dueDate == null) return "";
        java.time.temporal.WeekFields iso = java.time.temporal.WeekFields.ISO;
        return "Tuần " + dueDate.get(iso.weekOfWeekBasedYear()) + "/" + dueDate.get(iso.weekBasedYear());
    }

    private boolean isDateWithinRange(Object value, LocalDate start, LocalDate end) {
        if (value == null || value.toString().isBlank() || "—".equals(value.toString())) return false;
        LocalDate date = LocalDate.parse(value.toString());
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean inRange(LocalDateTime dateTime, LocalDate start, LocalDate end) {
        LocalDate date = dateTime.toLocalDate();
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean hasAdminDeadlineCorrection(ReportWork work) {
        return work.getExtensions().stream().anyMatch(extension -> "ADMIN_CORRECTION".equals(extension.getChangeType()));
    }

    private boolean isCompletedBy(ReportWork work, LocalDate cutoff) {
        return work.getCompletedDate() != null && !work.getCompletedDate().isAfter(cutoff);
    }

    private boolean periodTouchesRange(String period, LocalDate start, LocalDate end) {
        LocalDate periodStart = periodStart(period);
        LocalDate periodEnd = periodEnd(period);
        return !periodEnd.isBefore(start) && !periodStart.isAfter(end);
    }

    private String dateText(LocalDate date) { return date == null ? "—" : date.toString(); }

    private LocalDate periodEnd(String period) {
        if (period.matches("\\d{4}")) return LocalDate.of(Integer.parseInt(period), 12, 31);
        if (period.contains("W")) return periodStart(period).plusDays(6);
        if (period.contains("Q")) {
            int quarter = Integer.parseInt(period.substring(period.length() - 1));
            return LocalDate.of(Integer.parseInt(period.substring(0, 4)), quarter * 3, 1).withDayOfMonth(LocalDate.of(Integer.parseInt(period.substring(0, 4)), quarter * 3, 1).lengthOfMonth());
        }
        int year = Integer.parseInt(period.substring(0, 4));
        int month = Integer.parseInt(period.substring(5));
        return LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
    }

    private LocalDate periodStart(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        if (period.matches("\\d{4}")) return LocalDate.of(year, 1, 1);
        if (period.contains("W")) {
            int week = Integer.parseInt(period.substring(period.indexOf('W') + 1));
            LocalDate jan4 = LocalDate.of(year, 1, 4);
            return jan4.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).plusWeeks(week - 1L);
        }
        if (period.contains("Q")) return LocalDate.of(year, (Integer.parseInt(period.substring(period.length() - 1)) - 1) * 3 + 1, 1);
        return LocalDate.of(year, Integer.parseInt(period.substring(5)), 1);
    }

    private String kindLabel(String kind) {
        return switch (kind == null ? "ISSUE" : kind) { case "RISK" -> "Rủi ro"; case "ACTION" -> "Cần hỗ trợ / hành động"; default -> "Vướng mắc"; };
    }

    private String statusLabel(String status) {
        return switch (status == null ? "OPEN" : status) { case "IN_PROGRESS" -> "Đang xử lý"; case "RESOLVED" -> "Đã giải quyết"; case "CLOSED" -> "Đã đóng"; default -> "Mới ghi nhận"; };
    }

    private String dateRangeLabel(LocalDate start, LocalDate end) {
        return start + " – " + end;
    }

    private String periodLabel(String period) {
        if (period.matches("\\d{4}")) return "Năm " + period;
        if (period.contains("W")) return "Tuần " + Integer.parseInt(period.substring(period.indexOf('W') + 1)) + "/" + period.substring(0, 4);
        return period.contains("Q") ? "Quý " + period.substring(period.length() - 1) + "/" + period.substring(0, 4)
                : "Tháng " + Integer.parseInt(period.substring(5)) + "/" + period.substring(0, 4);
    }

    private int average(int[] values) {
        return values.length == 0 ? 0 : (int) Math.round(java.util.Arrays.stream(values).average().orElse(0));
    }
}
