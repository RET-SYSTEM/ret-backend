package me.cbhud.ret.dto.worker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps the JSON response returned by the Python FastAPI worker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerResponse {

    private String iic;
    private String dateTimeCreated;
    private String sellerName;
    private BigDecimal totalPrice;
    private String paymentMethod;
    private List<WorkerItemResponse> items;
}
