package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.service.report.ReportExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/report-work")
@RequiredArgsConstructor
public class ReportExcelController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ReportExcelService service;

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String period,
                                         @RequestParam(required = false) LocalDate startDate,
                                         @RequestParam(required = false) LocalDate endDate,
                                         @RequestParam(required = false) LocalDate cutoffDate) {
        byte[] workbook = service.export(period, startDate, endDate, cutoffDate);
        String filePeriod = startDate != null && endDate != null ? startDate + "_" + endDate : period;
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("Bao_cao_" + filePeriod + ".xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX)
                .contentLength(workbook.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.noStore())
                .body(workbook);
    }
}
