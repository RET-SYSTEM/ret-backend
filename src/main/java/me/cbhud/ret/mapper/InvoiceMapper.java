package me.cbhud.ret.mapper;

import me.cbhud.ret.dto.response.InvoiceItemResponse;
import me.cbhud.ret.dto.response.InvoiceResponse;
import me.cbhud.ret.dto.worker.WorkerItemResponse;
import me.cbhud.ret.dto.worker.WorkerResponse;
import me.cbhud.ret.entity.Category;
import me.cbhud.ret.entity.Invoice;
import me.cbhud.ret.entity.InvoiceItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Centralised mapping between entities, DTOs, and external responses.
 */
@Component
public class InvoiceMapper {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ── Entity → Response DTO ────────────────────────────────────────
    public InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .iic(invoice.getIic())
                .dateTime(invoice.getDateTime())
                .totalAmount(invoice.getTotalAmount())
                .storeName(invoice.getStoreName())
                .isCard(invoice.getIsCard())
                .items(invoice.getItems() == null
                        ? Collections.emptyList()
                        : invoice.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .category(item.getCategory() != null ? item.getCategory().getName() : null)
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .build();
    }

    // ── Worker response → Entity ─────────────────────────────────────
    public Invoice fromWorkerResponse(WorkerResponse workerResponse,
                                      Map<String, Category> categoryMap) {
        Invoice invoice = Invoice.builder()
                .iic(workerResponse.getIic())
                .dateTime(parseDateTime(workerResponse.getDateTimeCreated()))
                .totalAmount(workerResponse.getTotalPrice())
                .storeName(workerResponse.getSellerName())
                .isCard("CARD".equalsIgnoreCase(workerResponse.getPaymentMethod()))
                .build();

        List<WorkerItemResponse> workerItems = workerResponse.getItems();
        if (workerItems != null) {
            for (WorkerItemResponse wi : workerItems) {
                Category category = wi.getCategory() != null
                        ? categoryMap.get(wi.getCategory().toLowerCase())
                        : null;

                InvoiceItem item = InvoiceItem.builder()
                        .itemName(wi.getName())
                        .category(category)
                        .price(wi.getUnitPriceAfterVat())
                        .quantity(wi.getQuantity())
                        .build();
                invoice.addItem(item);
            }
        }

        return invoice;
    }

    // ── Date parsing (handles all tax API offset quirks) ────────────
    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        // 1. Replace space before offset "…54 01:00" → "…54+01:00"
        String normalized = raw.replace(" ", "+");

        // 2. Insert colon into bare offsets "+0000" → "+00:00", "+0100" → "+01:00"
        normalized = normalized.replaceAll("([+-])(\\d{2})(\\d{2})$", "$1$2:$3");

        try {
            return LocalDateTime.parse(normalized, ISO_OFFSET);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(normalized, ISO_LOCAL);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Unable to parse dateTime: " + raw, e2);
            }
        }
    }
}
