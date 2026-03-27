package me.cbhud.ret.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.cbhud.ret.dto.request.ReceiptProcessRequest;
import me.cbhud.ret.dto.request.UpdateInvoiceItemRequest;
import me.cbhud.ret.dto.request.UpdateInvoiceRequest;
import me.cbhud.ret.dto.response.InvoiceProcessResult;
import me.cbhud.ret.dto.response.InvoiceResponse;
import me.cbhud.ret.service.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receiptsz")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ══════════════════════════════════════════════════════════════════
    //  POST /api/receipts/process
    // ══════════════════════════════════════════════════════════════════
    @PostMapping("/process")
    public ResponseEntity<InvoiceResponse> processReceipt(
            @Valid @RequestBody ReceiptProcessRequest request) {

        InvoiceProcessResult result = invoiceService.processReceipt(request);

        HttpStatus status = result.isAlreadyExisted() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.getInvoice());
    }

    // ══════════════════════════════════════════════════════════════════
    //  GET /api/receipts  —  Paginated invoice list (dashboard)
    // ══════════════════════════════════════════════════════════════════
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getAllReceipts(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
    }

    // ══════════════════════════════════════════════════════════════════
    //  GET /api/receipts/export  —  CSV download
    // ══════════════════════════════════════════════════════════════════
    @GetMapping("/export")
    public ResponseEntity<String> exportCsv() {
        String csv = invoiceService.exportCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipts_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ══════════════════════════════════════════════════════════════════
    //  PUT /api/receipts/{id}  —  Update invoice header
    // ══════════════════════════════════════════════════════════════════
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @RequestBody UpdateInvoiceRequest request) {

        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
    }

    // ══════════════════════════════════════════════════════════════════
    //  DELETE /api/receipts/{id}  —  Delete invoice and all items
    // ══════════════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════
    //  PUT /api/receipts/{invoiceId}/items/{itemId}  —  Update item
    // ══════════════════════════════════════════════════════════════════
    @PutMapping("/{invoiceId}/items/{itemId}")
    public ResponseEntity<InvoiceResponse> updateItem(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @RequestBody UpdateInvoiceItemRequest request) {

        return ResponseEntity.ok(invoiceService.updateItem(invoiceId, itemId, request));
    }

    // ══════════════════════════════════════════════════════════════════
    //  DELETE /api/receipts/{invoiceId}/items/{itemId}  —  Delete item
    // ══════════════════════════════════════════════════════════════════
    @DeleteMapping("/{invoiceId}/items/{itemId}")
    public ResponseEntity<InvoiceResponse> deleteItem(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId) {

        return ResponseEntity.ok(invoiceService.deleteItem(invoiceId, itemId));
    }
}

