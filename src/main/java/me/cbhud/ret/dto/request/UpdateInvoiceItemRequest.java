package me.cbhud.ret.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceItemRequest {
    private String itemName;
    private String category;
    private BigDecimal price;
    private BigDecimal quantity;
}
