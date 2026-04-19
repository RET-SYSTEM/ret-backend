package me.cbhud.ret.service;

import me.cbhud.ret.dto.request.GenerateReportRequest;
import me.cbhud.ret.dto.response.SpendingReportResponse;

import java.util.List;

public interface ReportService {

    /** Generate a new AI report for the selected months, save, and return it. */
    SpendingReportResponse generateReport(GenerateReportRequest request);

    /** Return all saved reports, newest first. */
    List<SpendingReportResponse> getAllReports();

    /** Return a single saved report by id. */
    SpendingReportResponse getReport(Long id);
}
