package me.cbhud.ret.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.cbhud.ret.dto.request.GenerateReportRequest;
import me.cbhud.ret.dto.response.SpendingReportResponse;
import me.cbhud.ret.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ══════════════════════════════════════════════════════════════════
    //  POST /api/reports/generate
    // ══════════════════════════════════════════════════════════════════
    @PostMapping("/generate")
    public ResponseEntity<SpendingReportResponse> generate(
            @Valid @RequestBody GenerateReportRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.generateReport(request));
    }

    // ══════════════════════════════════════════════════════════════════
    //  GET /api/reports
    // ══════════════════════════════════════════════════════════════════
    @GetMapping
    public ResponseEntity<List<SpendingReportResponse>> getAll() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    // ══════════════════════════════════════════════════════════════════
    //  GET /api/reports/{id}
    // ══════════════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<SpendingReportResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReport(id));
    }
}
