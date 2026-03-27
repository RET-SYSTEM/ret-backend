package me.cbhud.ret.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unified request DTO that handles all three receipt-processing flows:
 * <ul>
 *   <li><b>Plan A</b> — QR scan: populate {@code url} only.</li>
 *   <li><b>Plan B</b> — Manual receipt fields: populate {@code iic}, {@code tin}, {@code dateTimeCreated}.</li>
 *   <li><b>Plan C</b> — No receipt / manual item entry: set {@code manualEntry = true}
 *       and supply {@code items} (plus optional {@code storeName}, {@code totalAmount}, {@code isCard}).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptProcessRequest {

    // ── Plan A: QR URL ──────────────────────────────────────────────
    private String url;

    // ── Plan B: Manual receipt fields ───────────────────────────────
    private String iic;
    private String tin;
    private String dateTimeCreated;

    // ── Plan C: Full manual entry (no receipt) ──────────────────────
    private Boolean manualEntry;
    private String storeName;
    private BigDecimal totalAmount;
    private Boolean isCard;

    @Valid
    private List<ManualItemRequest> items;
}
