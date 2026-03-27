package me.cbhud.ret.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoices_iic", columnNames = "iic")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal Invoice Code — the primary deduplication key.
     * Nullable to support manual entry (Plan C) where no receipt was scanned.
     */
    @Column(name = "iic")
    private String iic;

    @Column(name = "date_time")
    private LocalDateTime dateTime;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "is_card")
    private Boolean isCard;

    @Builder.Default
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    // ── Convenience helper ────────────────────────────────────────────
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }
}
