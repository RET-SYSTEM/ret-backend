package me.cbhud.ret.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Single item payload for Plan C (manual entry without a receipt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualItemRequest {

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String category;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;
}
