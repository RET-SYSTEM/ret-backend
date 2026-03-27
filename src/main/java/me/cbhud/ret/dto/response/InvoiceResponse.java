package me.cbhud.ret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public-facing representation of a saved invoice.
 * Never exposes raw JPA entities to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private String iic;
    private LocalDateTime dateTime;
    private BigDecimal totalAmount;
    private String storeName;
    private Boolean isCard;
    private List<InvoiceItemResponse> items;
}
