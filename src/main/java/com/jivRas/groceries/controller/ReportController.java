package com.jivRas.groceries.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.*;
import com.jivRas.groceries.service.DynamicAuthorizationService;
import com.jivRas.groceries.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for the Reports tab.
 *
 * All endpoints check DynamicAuthorizationService so permissions are managed
 * in the role_permissions table — same pattern as OrderController.
 *
 * Suggested role_permissions rows to seed:
 *   ADMIN          /api/reports/**  GET  true
 *   BRANCH_MANAGER /api/reports/**  GET  true
 *   EMPLOYEE       /api/reports/**  GET  false  (no access)
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final DynamicAuthorizationService dynamicAuthorizationService;

    // ── KPI Summary ───────────────────────────────────────────────────

    /**
     * GET /api/reports/summary
     * Today's total revenue, order count, top item, top category, low stock count.
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            HttpServletRequest req, Authentication auth) {

        if (!isAllowed(req, auth)) return forbidden();
        return ResponseEntity.ok(reportService.getSummary());
    }

    // ── Sales Trend ───────────────────────────────────────────────────

    /**
     * GET /api/reports/sales-trend?from=2026-03-01&to=2026-04-04
     * Daily revenue + order count for the given date range (default: last 30 days).
     */
    @GetMapping("/sales-trend")
    public ResponseEntity<?> getSalesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest req, Authentication auth) {

        if (!isAllowed(req, auth)) return forbidden();
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(reportService.getSalesTrend(start, end));
    }

    // ── Top Products ──────────────────────────────────────────────────

    /**
     * GET /api/reports/top-products?from=&to=&limit=10
     * Top N products by kg sold in the date range.
     */
    @GetMapping("/top-products")
    public ResponseEntity<?> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest req, Authentication auth) {

        if (!isAllowed(req, auth)) return forbidden();
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(reportService.getTopProducts(start, end, limit));
    }

    // ── Category Breakdown ────────────────────────────────────────────

    /**
     * GET /api/reports/category-breakdown?from=&to=
     * Revenue and volume per product category.
     */
    @GetMapping("/category-breakdown")
    public ResponseEntity<?> getCategoryBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest req, Authentication auth) {

        if (!isAllowed(req, auth)) return forbidden();
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(29);
        return ResponseEntity.ok(reportService.getCategoryBreakdown(start, end));
    }

    // ── Branch Comparison ─────────────────────────────────────────────

    /**
     * GET /api/reports/branch-comparison
     * Inventory health stats for every branch.
     */
    @GetMapping("/branch-comparison")
    public ResponseEntity<?> getBranchComparison(
            HttpServletRequest req, Authentication auth) {

        if (!isAllowed(req, auth)) return forbidden();
        return ResponseEntity.ok(reportService.getBranchComparison());
    }

    // ── Stock Alerts ──────────────────────────────────────────────────

    /**
     * GET /api/reports/low-stock
     * All branch-product entries below their threshold, with projected stockout.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStock(
            HttpServletRequest req, Authentication auth) {

        if (!isAllowed(req, auth)) return forbidden();
        return ResponseEntity.ok(reportService.getStockAlerts());
    }

    // ── Excel Export ──────────────────────────────────────────────────

    /**
     * GET /api/reports/export/excel?from=&to=
     * Downloads a three-sheet .xlsx: Order Summary, Product Sales, Stock Alerts.
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest req, Authentication auth) {

        if (!isAllowed(req, auth)) {
            return ResponseEntity.status(403).build();
        }
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

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private boolean isAllowed(HttpServletRequest req, Authentication auth) {
        String role = (auth != null && auth.isAuthenticated())
                ? auth.getAuthorities().stream()
                        .map(a -> a.getAuthority()).findFirst().orElse("")
                : "";
        return dynamicAuthorizationService.isAllowed(role, req.getRequestURI(), req.getMethod());
    }

    private ResponseEntity<String> forbidden() {
        return ResponseEntity.status(403).body("Access denied");
    }
}
