package com.goldresto.controller;

import com.goldresto.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAuthority('ROLE_OWNER')")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public String showDashboard(Model model) {
        LocalDate today = LocalDate.now();
        Map<String, Object> dailyReport = reportService.getDailySalesReport(today);
        Map<String, Object> monthlyReport = reportService.getPeriodSalesReport(
            today.withDayOfMonth(1), today, "month");
        Map<String, Object> productReport = reportService.getProductSalesReport(
            today.minusMonths(1), today);
        Map<String, Object> categoryReport = reportService.getCategorySalesReport(
            today.minusMonths(1), today);

        model.addAttribute("dailyReport", dailyReport);
        model.addAttribute("monthlyReport", monthlyReport);
        model.addAttribute("topProducts", productReport.get("topProducts"));
        model.addAttribute("categoryPerformance", categoryReport.get("categoryPerformance"));
        
        return "reports/dashboard";
    }

    @GetMapping("/daily")
    public String showDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        if (date == null) date = LocalDate.now();
        model.addAttribute("report", reportService.getDailySalesReport(date));
        return "reports/daily";
    }

    @GetMapping("/period")
    public String showPeriodReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String periodType,
            Model model) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        
        // Ensure endDate is not before startDate
        if (endDate.isBefore(startDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        // Validate periodType
        if (!periodType.matches("^(day|week|month|year)$")) {
            periodType = "day";
        }

        Map<String, Object> report = reportService.getPeriodSalesReport(startDate, endDate, periodType);
        model.addAttribute("report", report);
        model.addAttribute("periodType", periodType);
        return "reports/period";
    }

    @GetMapping("/products")
    public String showProductReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        // Ensure endDate is not before startDate
        if (endDate.isBefore(startDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        Map<String, Object> report = reportService.getProductSalesReport(startDate, endDate);
        model.addAttribute("report", report);
        return "reports/products";
    }

    @GetMapping("/categories")
    public String showCategoryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        model.addAttribute("report", reportService.getCategorySalesReport(startDate, endDate));
        return "reports/categories";
    }

    @GetMapping("/employees")
    public String showEmployeeReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        model.addAttribute("report", reportService.getEmployeePerformanceReport(startDate, endDate));
        return "reports/employees";
    }

    // API endpoints for chart data
    @GetMapping("/api/trends")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String periodType) {
        
        Map<String, Object> report = reportService.getPeriodSalesReport(startDate, endDate, periodType);
        return ResponseEntity.ok(report);
    }
}
