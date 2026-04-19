package me.cbhud.ret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public-facing representation of a saved AI spending report.
 * Never exposes raw JPA entities to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingReportResponse {

    private Long id;
    private LocalDateTime createdAt;
    private String title;
    private String selectedMonths;
    private List<String> summary;
    private List<String> anomalies;
    private List<String> tips;
}
