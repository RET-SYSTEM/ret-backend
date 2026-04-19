package me.cbhud.ret.dto.worker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Maps the JSON response returned by the Python worker's /report endpoint.
 * summary, anomalies, tips are JSON arrays in the Python response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportWorkerResponse {

    private String title;
    private List<String> summary;
    private List<String> anomalies;
    private List<String> tips;
}
