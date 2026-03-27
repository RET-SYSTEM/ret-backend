package me.cbhud.ret.dto.worker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerItemResponse {

    private String name;
    private BigDecimal unitPriceAfterVat;
    private BigDecimal quantity;
    private String category;
}
