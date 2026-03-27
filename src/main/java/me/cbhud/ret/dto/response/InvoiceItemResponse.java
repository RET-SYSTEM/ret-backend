package me.cbhud.ret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemResponse {

    private Long id;
    private String itemName;
    private String category;
    private BigDecimal price;
    private BigDecimal quantity;
}
