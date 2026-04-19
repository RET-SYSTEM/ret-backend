package me.cbhud.ret.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReportRequest {

    @NotEmpty(message = "At least one month is required.")
    private List<@Pattern(regexp = "\\d{4}-\\d{2}", message = "Months must be in YYYY-MM format.") String> selectedMonths;
}
