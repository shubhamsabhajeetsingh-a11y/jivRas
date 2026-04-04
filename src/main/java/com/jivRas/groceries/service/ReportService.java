package com.jivRas.groceries.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jivRas.groceries.dto.*;
import com.jivRas.groceries.entity.BranchInventory;
import com.jivRas.groceries.entity.Order;
import com.jivRas.groceries.entity.OrderItem;
import com.jivRas.groceries.repository.BranchInventoryRepository;
import com.jivRas.groceries.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─────────────────────────────────────────────────────────────────
    // KPI Summary — today's numbers
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);

        List<Order> todayOrders = orderRepository.findWithItemsByDateRange(startOfDay, endOfDay);

        double totalRevenue = todayOrders.stream()
                .mapToDouble(Order::getTotalAmount).sum();
        long orderCount = todayOrders.size();

        // Flatten all items from today's orders
        List<OrderItem> todayItems = todayOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.toList());

        // Top selling item (most kg)
        String topItem = todayItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductName,
                        Collectors.summingDouble(OrderItem::getQuantityKg)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        // Top category by revenue
        String topCategory = todayItems.stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getCategory() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getProduct().getCategory().getName(),
                        Collectors.summingDouble(i -> i.getQuantityKg() * i.getPricePerKg())))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        int lowStockCount = branchInventoryRepository.findAllLowStock().size();

        return ReportSummaryResponse.builder()
                .totalRevenueToday(totalRevenue)
                .totalOrdersToday(orderCount)
                .topSellingItem(topItem)
                .topCategory(topCategory)
                .lowStockAlertsCount(lowStockCount)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // Sales Trend — daily revenue over a date range
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SalesTrendPoint> getSalesTrend(LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findWithItemsByDateRange(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        // Group by date string, accumulate revenue and count
        Map<String, List<Order>> byDate = orders.stream()
                .collect(Collectors.groupingBy(o ->
                        o.getOrderDate().toLocalDate().format(DATE_FMT)));

        // Build one point per day in the range (fill missing days with zero)
        List<SalesTrendPoint> trend = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            String key = cursor.format(DATE_FMT);
            List<Order> dayOrders = byDate.getOrDefault(key, List.of());
            trend.add(SalesTrendPoint.builder()
                    .date(key)
                    .revenue(dayOrders.stream().mapToDouble(Order::getTotalAmount).sum())
                    .orderCount(dayOrders.size())
                    .build());
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    // ─────────────────────────────────────────────────────────────────
    // Top Products — most ordered items by kg in date range
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, int limit) {
        List<Order> orders = orderRepository.findWithItemsByDateRange(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        return orders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getProductName))
                .entrySet().stream()
                .map(e -> {
                    List<OrderItem> items = e.getValue();
                    double totalQty = items.stream().mapToDouble(OrderItem::getQuantityKg).sum();
                    double totalRev = items.stream()
                            .mapToDouble(i -> i.getQuantityKg() * i.getPricePerKg()).sum();
                    return TopProductResponse.builder()
                            .productName(e.getKey())
                            .totalQuantityKg(totalQty)
                            .totalRevenue(totalRev)
                            .build();
                })
                .sorted(Comparator.comparingDouble(TopProductResponse::getTotalQuantityKg).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // Category Breakdown — revenue & volume per category
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getCategoryBreakdown(LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findWithItemsByDateRange(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        return orders.stream()
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getProduct() != null && i.getProduct().getCategory() != null)
                .collect(Collectors.groupingBy(i -> i.getProduct().getCategory().getName()))
                .entrySet().stream()
                .map(e -> {
                    List<OrderItem> items = e.getValue();
                    double rev = items.stream()
                            .mapToDouble(i -> i.getQuantityKg() * i.getPricePerKg()).sum();
                    double qty = items.stream().mapToDouble(OrderItem::getQuantityKg).sum();
                    // count distinct orders containing this category
                    long ordersCount = items.stream()
                            .map(i -> i.getOrder().getId()).distinct().count();
                    return CategoryBreakdownResponse.builder()
                            .categoryName(e.getKey())
                            .totalRevenue(rev)
                            .totalQuantityKg(qty)
                            .orderCount(ordersCount)
                            .build();
                })
                .sorted(Comparator.comparingDouble(CategoryBreakdownResponse::getTotalRevenue).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // Branch Comparison — inventory health per branch
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BranchStockComparison> getBranchComparison() {
        List<BranchInventory> all = branchInventoryRepository.findAll();

        return all.stream()
                .collect(Collectors.groupingBy(bi -> bi.getBranch().getId()))
                .entrySet().stream()
                .map(e -> {
                    List<BranchInventory> items = e.getValue();
                    String branchName = items.get(0).getBranch().getName();
                    int total    = items.size();
                    int lowCount = (int) items.stream()
                            .filter(bi -> bi.getAvailableStockKg() != null
                                    && bi.getLowStockThreshold() != null
                                    && bi.getAvailableStockKg() <= bi.getLowStockThreshold())
                            .count();
                    double totalStock = items.stream()
                            .mapToDouble(BranchInventory::getAvailableStockKg).sum();
                    double pct = total > 0 ? (lowCount * 100.0 / total) : 0;
                    return BranchStockComparison.builder()
                            .branchId(e.getKey())
                            .branchName(branchName)
                            .totalProducts(total)
                            .lowStockCount(lowCount)
                            .totalStockKg(totalStock)
                            .lowStockPercentage(Math.round(pct * 10.0) / 10.0)
                            .build();
                })
                .sorted(Comparator.comparing(BranchStockComparison::getBranchName))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // Stock Alerts — low stock items with projected stockout
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StockAlertItem> getStockAlerts() {
        List<BranchInventory> lowStock = branchInventoryRepository.findAllLowStock();

        // Build daily consumption map: productName -> avg kg/day over last 30 days
        LocalDate today = LocalDate.now();
        List<Order> last30 = orderRepository.findWithItemsByDateRange(
                today.minusDays(30).atStartOfDay(), today.plusDays(1).atStartOfDay());

        Map<String, Double> avgDailyConsumption = last30.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::getProductName,
                        Collectors.summingDouble(OrderItem::getQuantityKg)))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() / 30.0));

        return lowStock.stream()
                .map(bi -> {
                    String productName = bi.getProduct().getName();
                    String catName = (bi.getProduct().getCategory() != null)
                            ? bi.getProduct().getCategory().getName() : "Unknown";

                    double stock = bi.getAvailableStockKg() != null ? bi.getAvailableStockKg() : 0;
                    String severity = stock < 3.0 ? "CRITICAL" : "WARNING";

                    Double daysLeft = null;
                    double dailyRate = avgDailyConsumption.getOrDefault(productName, 0.0);
                    if (dailyRate > 0) {
                        daysLeft = Math.round((stock / dailyRate) * 10.0) / 10.0;
                    }

                    return StockAlertItem.builder()
                            .branchId(bi.getBranch().getId())
                            .branchName(bi.getBranch().getName())
                            .productId(bi.getProduct().getId())
                            .productName(productName)
                            .categoryName(catName)
                            .availableStockKg(stock)
                            .lowStockThreshold(bi.getLowStockThreshold() != null
                                    ? bi.getLowStockThreshold() : 5.0)
                            .severity(severity)
                            .projectedDaysLeft(daysLeft)
                            .build();
                })
                .sorted(Comparator.comparingDouble(StockAlertItem::getAvailableStockKg))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // Excel Export — full sales report for a date range
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportExcel(LocalDate from, LocalDate to) throws IOException {
        List<Order> orders = orderRepository.findWithItemsByDateRange(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // ── Sheet 1: Order Summary ────────────────────────────
            Sheet summarySheet = wb.createSheet("Order Summary");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row headerRow = summarySheet.createRow(0);
            String[] headers = {"Order ID", "Date", "Customer", "City", "Status",
                                 "Total (₹)", "Items"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Order o : orders) {
                Row row = summarySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(o.getId());
                row.createCell(1).setCellValue(
                        o.getOrderDate().toLocalDate().format(DATE_FMT));
                row.createCell(2).setCellValue(o.getCustomerName());
                row.createCell(3).setCellValue(o.getCity());
                row.createCell(4).setCellValue(o.getOrderStatus());
                row.createCell(5).setCellValue(o.getTotalAmount());
                row.createCell(6).setCellValue(o.getItems().size());
            }
            autoSizeColumns(summarySheet, headers.length);

            // ── Sheet 2: Product Sales ────────────────────────────
            Sheet prodSheet = wb.createSheet("Product Sales");
            Row prodHeader = prodSheet.createRow(0);
            String[] prodHeaders = {"Product", "Category", "Total Qty (kg)",
                                    "Revenue (₹)", "Orders"};
            for (int i = 0; i < prodHeaders.length; i++) {
                Cell c = prodHeader.createCell(i);
                c.setCellValue(prodHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            List<TopProductResponse> topProds = getTopProducts(from, to, Integer.MAX_VALUE);
            // Build category lookup from items
            Map<String, String> productCategory = orders.stream()
                    .flatMap(o -> o.getItems().stream())
                    .filter(i -> i.getProduct() != null && i.getProduct().getCategory() != null)
                    .collect(Collectors.toMap(
                            OrderItem::getProductName,
                            i -> i.getProduct().getCategory().getName(),
                            (a, b) -> a));

            int prodRow = 1;
            for (TopProductResponse p : topProds) {
                Row row = prodSheet.createRow(prodRow++);
                row.createCell(0).setCellValue(p.getProductName());
                row.createCell(1).setCellValue(
                        productCategory.getOrDefault(p.getProductName(), "Unknown"));
                row.createCell(2).setCellValue(p.getTotalQuantityKg());
                row.createCell(3).setCellValue(p.getTotalRevenue());
                // order count: number of orders containing this product
                long oc = orders.stream()
                        .filter(o -> o.getItems().stream()
                                .anyMatch(i -> i.getProductName().equals(p.getProductName())))
                        .count();
                row.createCell(4).setCellValue(oc);
            }
            autoSizeColumns(prodSheet, prodHeaders.length);

            // ── Sheet 3: Stock Alerts ─────────────────────────────
            Sheet alertSheet = wb.createSheet("Stock Alerts");
            Row alertHeader = alertSheet.createRow(0);
            String[] alertHeaders = {"Branch", "Product", "Category",
                                     "Stock (kg)", "Threshold (kg)", "Severity",
                                     "Est. Days Left"};
            for (int i = 0; i < alertHeaders.length; i++) {
                Cell c = alertHeader.createCell(i);
                c.setCellValue(alertHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            List<StockAlertItem> alerts = getStockAlerts();
            int alertRow = 1;
            for (StockAlertItem a : alerts) {
                Row row = alertSheet.createRow(alertRow++);
                row.createCell(0).setCellValue(a.getBranchName());
                row.createCell(1).setCellValue(a.getProductName());
                row.createCell(2).setCellValue(a.getCategoryName());
                row.createCell(3).setCellValue(a.getAvailableStockKg());
                row.createCell(4).setCellValue(a.getLowStockThreshold());
                row.createCell(5).setCellValue(a.getSeverity());
                row.createCell(6).setCellValue(
                        a.getProjectedDaysLeft() != null ? a.getProjectedDaysLeft() : -1);
            }
            autoSizeColumns(alertSheet, alertHeaders.length);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
