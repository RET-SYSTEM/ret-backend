package me.cbhud.ret.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.cbhud.ret.dto.request.GenerateReportRequest;
import me.cbhud.ret.dto.response.SpendingReportResponse;
import me.cbhud.ret.dto.worker.ReportWorkerResponse;
import me.cbhud.ret.entity.Invoice;
import me.cbhud.ret.entity.InvoiceItem;
import me.cbhud.ret.entity.SpendingReport;
import me.cbhud.ret.exception.WorkerServiceException;
import me.cbhud.ret.repository.InvoiceRepository;
import me.cbhud.ret.repository.SpendingReportRepository;
import me.cbhud.ret.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final InvoiceRepository invoiceRepository;
    private final SpendingReportRepository spendingReportRepository;
    private final RestClient workerRestClient;
    private final ObjectMapper objectMapper;

    // ══════════════════════════════════════════════════════════════════
    //  GENERATE
    // ══════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public SpendingReportResponse generateReport(GenerateReportRequest request) {
        List<String> months = request.getSelectedMonths();
        log.info("Generating report for months: {}", months);

        // 1 — Load all invoices (with items) and filter by selected months
        List<Invoice> allInvoices = invoiceRepository.findAllByOrderByDateTimeDesc();
        List<Invoice> filtered = allInvoices.stream()
                .filter(inv -> inv.getDateTime() != null)
                .filter(inv -> months.contains(toYearMonth(inv.getDateTime())))
                .toList();

        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("No invoices found for the selected months: " + months);
        }

        // 2 — Build structured analytics payload
        Map<String, Object> payload = buildAnalyticsPayload(filtered, months);

        // 3 — Call Python worker
        ReportWorkerResponse workerResponse = callWorkerReport(payload);

        // 4 — Serialise list fields to JSON strings for TEXT columns
        String rawJson      = serialiseToJson(payload);
        String summaryJson  = serialiseToJson(workerResponse.getSummary());
        String anomaliesJson = serialiseToJson(workerResponse.getAnomalies());
        String tipsJson     = serialiseToJson(workerResponse.getTips());

        // Use title from worker if present, fall back to generated title
        String title = (workerResponse.getTitle() != null && !workerResponse.getTitle().isBlank())
                ? workerResponse.getTitle()
                : buildTitle(months);

        // 5 — Save report
        SpendingReport report = SpendingReport.builder()
                .createdAt(LocalDateTime.now())
                .title(title)
                .selectedMonths(String.join(",", months))
                .summary(summaryJson)
                .anomalies(anomaliesJson)
                .tips(tipsJson)
                .rawResponseJson(rawJson)
                .build();

        report = spendingReportRepository.save(report);
        log.info("Saved spending report id={}", report.getId());
        return toResponse(report);
    }

    // ══════════════════════════════════════════════════════════════════
    //  LIST / GET
    // ══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public List<SpendingReportResponse> getAllReports() {
        return spendingReportRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpendingReportResponse getReport(Long id) {
        SpendingReport report = spendingReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Report with id " + id + " not found."));
        return toResponse(report);
    }

    // ══════════════════════════════════════════════════════════════════
    //  ANALYTICS BUILDER  (backend does math, Python does wording)
    // ══════════════════════════════════════════════════════════════════
    private Map<String, Object> buildAnalyticsPayload(List<Invoice> invoices, List<String> months) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("selectedMonths", months);

        // ── Per-month stats ─────────────────────────────────────────
        List<Map<String, Object>> monthlyStats = new ArrayList<>();
        for (String month : months) {
            List<Invoice> monthInvoices = invoices.stream()
                    .filter(inv -> toYearMonth(inv.getDateTime()).equals(month))
                    .toList();

            Map<String, Object> ms = new LinkedHashMap<>();
            ms.put("month", month);
            ms.put("receiptCount", monthInvoices.size());

            BigDecimal total = monthInvoices.stream()
                    .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ms.put("totalSpending", total);

            BigDecimal avg = monthInvoices.isEmpty() ? BigDecimal.ZERO
                    : total.divide(BigDecimal.valueOf(monthInvoices.size()), 2, RoundingMode.HALF_UP);
            ms.put("averageReceiptValue", avg);

            // Category totals for this month
            Map<String, BigDecimal> catTotals = new LinkedHashMap<>();
            for (Invoice inv : monthInvoices) {
                for (InvoiceItem item : inv.getItems()) {
                    String cat = item.getCategory() != null ? item.getCategory().getName() : "Uncategorized";
                    BigDecimal itemTotal = itemTotal(item);
                    catTotals.merge(cat, itemTotal, BigDecimal::add);
                }
            }
            // Sort descending by value for readability
            List<Map<String, Object>> catList = catTotals.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .map(e -> Map.<String, Object>of("category", e.getKey(), "total", e.getValue()))
                    .toList();
            ms.put("categoryTotals", catList);

            // Weekday vs weekend
            long weekdayCount = monthInvoices.stream()
                    .filter(inv -> !isWeekend(inv.getDateTime())).count();
            long weekendCount = monthInvoices.size() - weekdayCount;
            BigDecimal weekdaySpend = monthInvoices.stream()
                    .filter(inv -> !isWeekend(inv.getDateTime()))
                    .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal weekendSpend = total.subtract(weekdaySpend);
            ms.put("weekdayReceipts", weekdayCount);
            ms.put("weekendReceipts", weekendCount);
            ms.put("weekdaySpending", weekdaySpend);
            ms.put("weekendSpending", weekendSpend);

            // Time-of-day distribution
            Map<String, Long> timeOfDay = new LinkedHashMap<>();
            timeOfDay.put("morning",   monthInvoices.stream().filter(inv -> hourOf(inv) >= 5  && hourOf(inv) < 12).count());
            timeOfDay.put("afternoon", monthInvoices.stream().filter(inv -> hourOf(inv) >= 12 && hourOf(inv) < 17).count());
            timeOfDay.put("evening",   monthInvoices.stream().filter(inv -> hourOf(inv) >= 17 && hourOf(inv) < 22).count());
            timeOfDay.put("night",     monthInvoices.stream().filter(inv -> hourOf(inv) >= 22 || hourOf(inv) < 5).count());
            ms.put("timeOfDayDistribution", timeOfDay);

            monthlyStats.add(ms);
        }
        payload.put("monthlyStats", monthlyStats);

        // ── Top 5 biggest receipts across all selected months ───────
        List<Map<String, Object>> biggestReceipts = invoices.stream()
                .filter(inv -> inv.getTotalAmount() != null)
                .sorted(Comparator.comparing(Invoice::getTotalAmount).reversed())
                .limit(5)
                .map(inv -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("storeName", inv.getStoreName());
                    r.put("total", inv.getTotalAmount());
                    r.put("date", inv.getDateTime().toLocalDate().toString());
                    r.put("isCard", inv.getIsCard());
                    return r;
                })
                .toList();
        payload.put("biggestReceipts", biggestReceipts);

        // ── Top 5 categories overall ────────────────────────────────
        Map<String, BigDecimal> overallCat = new LinkedHashMap<>();
        for (Invoice inv : invoices) {
            for (InvoiceItem item : inv.getItems()) {
                String cat = item.getCategory() != null ? item.getCategory().getName() : "Uncategorized";
                overallCat.merge(cat, itemTotal(item), BigDecimal::add);
            }
        }
        List<Map<String, Object>> topCategories = overallCat.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> Map.<String, Object>of("category", e.getKey(), "total", e.getValue()))
                .toList();
        payload.put("topCategories", topCategories);

        // ── Suspiciously similar receipts ───────────────────────────
        // Same store + same total appearing more than once across selected months
        Map<String, Long> duplicateCandidates = invoices.stream()
                .filter(inv -> inv.getStoreName() != null && inv.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        inv -> inv.getStoreName().toLowerCase() + "|" + inv.getTotalAmount().toPlainString(),
                        Collectors.counting()
                ));
        List<Map<String, Object>> suspicious = duplicateCandidates.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> {
                    String[] parts = e.getKey().split("\\|");
                    return Map.<String, Object>of(
                            "storeName", parts[0],
                            "amount", parts[1],
                            "occurrences", e.getValue()
                    );
                })
                .toList();
        payload.put("suspiciouslySimilarReceipts", suspicious);

        // ── Month-over-month comparison (if >1 month) ───────────────
        if (months.size() > 1) {
            List<Map<String, Object>> comparison = new ArrayList<>();
            for (int i = 1; i < months.size(); i++) {
                String prev = months.get(i - 1);
                String curr = months.get(i);
                BigDecimal prevTotal = totalForMonth(invoices, prev);
                BigDecimal currTotal = totalForMonth(invoices, curr);
                BigDecimal diff = currTotal.subtract(prevTotal);
                comparison.add(Map.of(
                        "from", prev,
                        "to", curr,
                        "previousTotal", prevTotal,
                        "currentTotal", currTotal,
                        "difference", diff
                ));
            }
            payload.put("monthComparison", comparison);
        }

        return payload;
    }

    // ══════════════════════════════════════════════════════════════════
    //  WORKER CALL
    // ══════════════════════════════════════════════════════════════════
    private ReportWorkerResponse callWorkerReport(Map<String, Object> payload) {
        try {
            ReportWorkerResponse response = workerRestClient.post()
                    .uri("/report")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(ReportWorkerResponse.class);

            if (response == null) {
                throw new WorkerServiceException("Python worker returned an empty report response.");
            }
            return response;
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            log.error("Python worker returned HTTP {} for /report: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new WorkerServiceException(
                    "AI report generation failed: worker returned HTTP " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("Failed to reach Python worker for report: {}", ex.getMessage());
            throw new WorkerServiceException(
                    "Python worker is unreachable or returned an error during report generation.", ex);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════
    private String toYearMonth(LocalDateTime dt) {
        return YearMonth.from(dt).toString(); // "YYYY-MM"
    }

    private boolean isWeekend(LocalDateTime dt) {
        if (dt == null) return false;
        var day = dt.getDayOfWeek();
        return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
    }

    private int hourOf(Invoice inv) {
        return inv.getDateTime() != null ? inv.getDateTime().getHour() : 0;
    }

    private BigDecimal itemTotal(InvoiceItem item) {
        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        BigDecimal qty   = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
        return price.multiply(qty);
    }

    private BigDecimal totalForMonth(List<Invoice> invoices, String month) {
        return invoices.stream()
                .filter(inv -> toYearMonth(inv.getDateTime()).equals(month))
                .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildTitle(List<String> months) {
        if (months.size() == 1) return "Spending Report — " + months.get(0);
        return "Spending Report — " + months.get(0) + " to " + months.get(months.size() - 1);
    }

    private String serialiseToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise analytics payload to JSON", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private SpendingReportResponse toResponse(SpendingReport report) {
        return SpendingReportResponse.builder()
                .id(report.getId())
                .createdAt(report.getCreatedAt())
                .title(report.getTitle())
                .selectedMonths(report.getSelectedMonths())
                .summary(deserialiseList(report.getSummary()))
                .anomalies(deserialiseList(report.getAnomalies()))
                .tips(deserialiseList(report.getTips()))
                .build();
    }

    /** Deserialise a JSON array string from the DB back to List<String>. */
    private List<String> deserialiseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("Could not deserialise list from JSON, returning raw string as single-element list.");
            return List.of(json);
        }
    }
}
