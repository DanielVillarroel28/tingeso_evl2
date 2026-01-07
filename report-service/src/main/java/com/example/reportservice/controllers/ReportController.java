package com.example.reportservice.controllers;

import com.example.reportservice.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/loans/active")
    public ResponseEntity<List<ReportService.LoanDTO>> getActiveLoans() {
        return ResponseEntity.ok(reportService.getActiveLoans());
    }

    @GetMapping("/clients/overdue")
    public ResponseEntity<List<ReportService.OverdueClientDTO>> getOverdueClients() {
        return ResponseEntity.ok(reportService.getOverdueClients());
    }

    @GetMapping("/tools/top")
    public ResponseEntity<List<ReportService.TopToolDTO>> getTopTools() {
        return ResponseEntity.ok(reportService.getTopTools());
    }
}