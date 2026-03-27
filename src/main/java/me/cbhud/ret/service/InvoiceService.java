package me.cbhud.ret.service;

import me.cbhud.ret.dto.request.ReceiptProcessRequest;
import me.cbhud.ret.dto.request.UpdateInvoiceItemRequest;
import me.cbhud.ret.dto.request.UpdateInvoiceRequest;
import me.cbhud.ret.dto.response.InvoiceProcessResult;
import me.cbhud.ret.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Core invoice business logic.
 */
public interface InvoiceService {

    /**
     * Process an incoming receipt (QR scan, manual receipt fields, or manual entry).
     * Implements the "check-first" deduplication strategy.
     */
    InvoiceProcessResult processReceipt(ReceiptProcessRequest request);

    /**
     * Paginated list of all invoices, newest first.
     */
    Page<InvoiceResponse> getAllInvoices(Pageable pageable);

    /**
     * CSV export: date, day of week, store name, item name, category, price.
     */
    String exportCsv();

    /**
     * Update invoice header fields (store name, total, card/cash, date).
     */
    InvoiceResponse updateInvoice(Long id, UpdateInvoiceRequest request);

    /**
     * Delete an invoice and all its items.
     */
    void deleteInvoice(Long id);

    /**
     * Update a single item on an invoice.
     */
    InvoiceResponse updateItem(Long invoiceId, Long itemId, UpdateInvoiceItemRequest request);

    /**
     * Delete a single item from an invoice.
     */
    InvoiceResponse deleteItem(Long invoiceId, Long itemId);
}

