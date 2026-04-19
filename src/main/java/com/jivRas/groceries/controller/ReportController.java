package com.jivRas.groceries.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.dto.*;
import com.jivRas.groceries.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @ModuleAction(module = "REPORTS", action = "VIEW")
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        return ResponseEntity.ok(reportService.getSummary());
    }

    @ModuleAction(module = "REPORTS", action = "VIEW")
    @GetMapping("/sales-trend")
    public ResponseEntity<?> getSalesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(reportService.getSalesTrend(start, end));
    }

    @ModuleAction(module = "REPORTS", action = "VIEW")
    @GetMapping("/top-products")
    public ResponseEntity<?> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(reportService.getTopProducts(start, end, limit));
    }

    @ModuleAction(module = "REPORTS", action = "VIEW")
    @GetMapping("/category-breakdown")
    public ResponseEntity<?> getCategoryBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(reportService.getCategoryBreakdown(start, end));
    }

    @ModuleAction(module = "REPORTS", action = "VIEW")
    @GetMapping("/branch-comparison")
    public ResponseEntity<?> getBranchComparison() {
        return ResponseEntity.ok(reportService.getBranchComparison());
    }

    @ModuleAction(module = "REPORTS", action = "VIEW")
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStock() {
        return ResponseEntity.ok(reportService.getStockAlerts());
    }

    @ModuleAction(module = "REPORTS", action = "VIEW")
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);

        try {
            byte[] data = reportService.exportExcel(start, end);
            String filename = "JivRas_Report_" + start + "_to_" + end + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
