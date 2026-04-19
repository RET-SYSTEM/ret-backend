package me.cbhud.ret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "spending_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Comma-separated month strings, e.g. "2024-01,2024-02".
     * Stored as plain TEXT — no migration needed with ddl-auto: update.
     */
    @Column(name = "selected_months", nullable = false, columnDefinition = "TEXT")
    private String selectedMonths;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "anomalies", columnDefinition = "TEXT")
    private String anomalies;

    @Column(name = "tips", columnDefinition = "TEXT")
    private String tips;

    /** Full raw JSON payload from the AI response, kept for future flexibility. */
    @Column(name = "raw_response_json", columnDefinition = "TEXT")
    private String rawResponseJson;
}
