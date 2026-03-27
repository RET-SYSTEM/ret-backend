package me.cbhud.ret.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.cbhud.ret.dto.request.ManualItemRequest;
import me.cbhud.ret.dto.request.ReceiptProcessRequest;
import me.cbhud.ret.dto.request.UpdateInvoiceItemRequest;
import me.cbhud.ret.dto.request.UpdateInvoiceRequest;
import me.cbhud.ret.dto.response.InvoiceProcessResult;
import me.cbhud.ret.dto.response.InvoiceResponse;
import me.cbhud.ret.dto.worker.WorkerResponse;
import me.cbhud.ret.entity.Category;
import me.cbhud.ret.entity.Invoice;
import me.cbhud.ret.entity.InvoiceItem;
import me.cbhud.ret.exception.DuplicateInvoiceException;
import me.cbhud.ret.exception.WorkerServiceException;
import me.cbhud.ret.mapper.InvoiceMapper;
import me.cbhud.ret.repository.CategoryRepository;
import me.cbhud.ret.repository.InvoiceItemRepository;
import me.cbhud.ret.repository.InvoiceRepository;
import me.cbhud.ret.service.InvoiceService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CategoryRepository categoryRepository;
    private final InvoiceMapper invoiceMapper;
    private final RestClient workerRestClient;

    // ══════════════════════════════════════════════════════════════════
    //  PROCESS RECEIPT (Plans A, B, C)
    // ══════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public InvoiceProcessResult processReceipt(ReceiptProcessRequest request) {

        // ── Plan C: manual item entry (no receipt) ───────────────────
        if (Boolean.TRUE.equals(request.getManualEntry())) {
            log.info("Processing manual entry (Plan C)");
            return new InvoiceProcessResult(processManualEntry(request), false);
        }

        // ── Extract iic / tin / dateTimeCreated ──────────────────────
        String iic;
        String tin;
        String dateTimeCreated;

        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            log.info("Processing QR URL (Plan A): {}", request.getUrl());
            Map<String, String> params = parseReceiptUrl(request.getUrl());
            iic             = params.get("iic");
            tin             = params.get("tin");
            dateTimeCreated = params.get("crtd");
        } else {
            log.info("Processing manual receipt fields (Plan B)");
            iic             = request.getIic();
            tin             = request.getTin();
            dateTimeCreated = request.getDateTimeCreated();
        }

        if (iic == null || iic.isBlank()) {
            throw new IllegalArgumentException(
                    "IIC is required for receipt processing. Use manualEntry=true for entries without a receipt.");
        }

        // ── Check-First: prevent unnecessary scraper calls ──────────
        Optional<Invoice> existing = invoiceRepository.findByIic(iic);
        if (existing.isPresent()) {
            log.info("Invoice with IIC '{}' already exists — returning cached record", iic);
            return new InvoiceProcessResult(invoiceMapper.toResponse(existing.get()), true);
        }

        // ── Call Python worker (pass current categories) ────────────
        List<String> categoryNames = categoryRepository.findAll().stream()
                .map(Category::getName)
                .toList();
        WorkerResponse workerResponse = callWorker(iic, tin, dateTimeCreated, categoryNames);

        // ── Map, save, return ───────────────────────────────────────
        Map<String, Category> categoryMap = buildCategoryMap();
        Invoice invoice = invoiceMapper.fromWorkerResponse(workerResponse, categoryMap);

        try {
            invoice = invoiceRepository.save(invoice);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition detected for IIC '{}' — fetching existing record", iic);
            throw new DuplicateInvoiceException(iic);
        }

        log.info("Saved new invoice id={} with {} items", invoice.getId(), invoice.getItems().size());
        return new InvoiceProcessResult(invoiceMapper.toResponse(invoice), false);
    }

    // ══════════════════════════════════════════════════════════════════
    //  LIST ALL INVOICES (paginated)
    // ══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAllByOrderByDateTimeDesc(pageable)
                .map(invoiceMapper::toResponse);
    }

    // ══════════════════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public String exportCsv() {
        List<Invoice> invoices = invoiceRepository.findAllByOrderByDateTimeDesc();

        StringBuilder csv = new StringBuilder();
        csv.append("date,day_of_week,store_name,item_name,category,price\n");

        for (Invoice inv : invoices) {
            String date      = inv.getDateTime() != null ? inv.getDateTime().toLocalDate().toString() : "";
            String dayOfWeek = inv.getDateTime() != null
                    ? inv.getDateTime().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) : "";
            String store     = escapeCsv(inv.getStoreName());

            for (InvoiceItem item : inv.getItems()) {
                String categoryName = item.getCategory() != null ? item.getCategory().getName() : "";
                csv.append(date).append(',')
                   .append(dayOfWeek).append(',')
                   .append(store).append(',')
                   .append(escapeCsv(item.getItemName())).append(',')
                   .append(escapeCsv(categoryName)).append(',')
                   .append(item.getPrice() != null ? item.getPrice().toPlainString() : "")
                   .append('\n');
            }
        }

        return csv.toString();
    }

    // ══════════════════════════════════════════════════════════════════
    //  UPDATE / DELETE
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InvoiceResponse updateInvoice(Long id, UpdateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice with id " + id + " not found."));

        if (request.getStoreName() != null) invoice.setStoreName(request.getStoreName());
        if (request.getTotalAmount() != null) invoice.setTotalAmount(request.getTotalAmount());
        if (request.getIsCard() != null) invoice.setIsCard(request.getIsCard());
        if (request.getDateTime() != null) invoice.setDateTime(request.getDateTime());

        invoice = invoiceRepository.save(invoice);
        log.info("Updated invoice id={}", invoice.getId());
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new EntityNotFoundException("Invoice with id " + id + " not found.");
        }
        invoiceRepository.deleteById(id);
        log.info("Deleted invoice id={}", id);
    }

    @Override
    @Transactional
    public InvoiceResponse updateItem(Long invoiceId, Long itemId, UpdateInvoiceItemRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice with id " + invoiceId + " not found."));

        InvoiceItem item = invoice.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Item with id " + itemId + " not found on invoice " + invoiceId + "."));

        if (request.getItemName() != null) item.setItemName(request.getItemName());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getCategory() != null) {
            Category category = categoryRepository.findByNameIgnoreCase(request.getCategory())
                    .orElse(null);
            item.setCategory(category);
        }

        recalculateTotal(invoice);
        invoiceRepository.save(invoice);
        log.info("Updated item id={} on invoice id={}", itemId, invoiceId);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse deleteItem(Long invoiceId, Long itemId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice with id " + invoiceId + " not found."));

        boolean removed = invoice.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new EntityNotFoundException(
                    "Item with id " + itemId + " not found on invoice " + invoiceId + ".");
        }

        recalculateTotal(invoice);
        invoiceRepository.save(invoice);
        log.info("Deleted item id={} from invoice id={}", itemId, invoiceId);
        return invoiceMapper.toResponse(invoice);
    }

    // ══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════

    /** Recalculates the totalAmount based on all items currently attached to the invoice. */
    private void recalculateTotal(Invoice invoice) {
        BigDecimal newTotal = invoice.getItems().stream()
                .map(item -> {
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                    return price.multiply(qty);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setTotalAmount(newTotal);
        log.debug("Recalculated total for invoice id={} to {}", invoice.getId(), newTotal);
    }

    /** Build a lowercase-keyed map of all categories for fast lookup. */
    private Map<String, Category> buildCategoryMap() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        c -> c.getName().toLowerCase(),
                        c -> c,
                        (a, b) -> a
                ));
    }

    /** Plan C — save manually entered items directly, bypassing the Python worker. */
    private InvoiceResponse processManualEntry(ReceiptProcessRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required for manual entry.");
        }

        Map<String, Category> categoryMap = buildCategoryMap();

        Invoice invoice = Invoice.builder()
                .storeName(request.getStoreName())
                .totalAmount(request.getTotalAmount())
                .isCard(request.getIsCard())
                .build();

        for (ManualItemRequest mi : request.getItems()) {
            Category category = mi.getCategory() != null
                    ? categoryMap.get(mi.getCategory().toLowerCase())
                    : null;

            InvoiceItem item = InvoiceItem.builder()
                    .itemName(mi.getItemName())
                    .category(category)
                    .price(mi.getPrice())
                    .quantity(mi.getQuantity())
                    .build();
            invoice.addItem(item);
        }

        invoice = invoiceRepository.save(invoice);
        log.info("Saved manual entry invoice id={} with {} items", invoice.getId(), invoice.getItems().size());
        return invoiceMapper.toResponse(invoice);
    }

    /** Calls the Python FastAPI worker with the current category list. */
    private WorkerResponse callWorker(String iic, String tin, String dateTimeCreated,
                                      List<String> categories) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("iic", iic);
            body.put("tin", tin != null ? tin : "");
            body.put("dateTimeCreated", dateTimeCreated != null ? dateTimeCreated : "");
            body.put("categories", categories);

            WorkerResponse response = workerRestClient.post()
                    .uri("/extract")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(WorkerResponse.class);

            if (response == null) {
                throw new WorkerServiceException("Python worker returned an empty response.");
            }

            return response;
        } catch (RestClientException ex) {
            log.error("Failed to reach Python worker: {}", ex.getMessage());
            throw new WorkerServiceException(
                    "Python worker is unreachable or returned an error. Ensure the worker is running.", ex);
        }
    }

    /** Parse a Montenegrin fiscal QR URL to extract iic, tin, crtd. */
    private Map<String, String> parseReceiptUrl(String url) {
        Map<String, String> params = new HashMap<>();

        String queryString;
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex != -1) {
            String fragment = url.substring(fragmentIndex + 1);
            int queryIndex = fragment.indexOf('?');
            queryString = queryIndex != -1 ? fragment.substring(queryIndex + 1) : "";
        } else {
            int queryIndex = url.indexOf('?');
            queryString = queryIndex != -1 ? url.substring(queryIndex + 1) : "";
        }

        for (String param : queryString.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                params.put(
                        URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            }
        }

        if (!params.containsKey("iic")) {
            throw new IllegalArgumentException(
                    "Invalid receipt URL — could not extract 'iic' parameter: " + url);
        }

        return params;
    }

    /** Wrap values that may contain commas or quotes for CSV safety. */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
