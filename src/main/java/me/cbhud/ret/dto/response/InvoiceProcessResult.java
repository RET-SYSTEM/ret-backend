package me.cbhud.ret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper so the controller can distinguish HTTP 200 (existing) from 201 (created).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProcessResult {

    private InvoiceResponse invoice;
    private boolean alreadyExisted;
}
