package me.cbhud.ret.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceRequest {
    private String storeName;
    private BigDecimal totalAmount;
    private Boolean isCard;
    private LocalDateTime dateTime;
}
