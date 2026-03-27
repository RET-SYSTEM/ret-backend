package me.cbhud.ret.repository;

import me.cbhud.ret.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Invoice> findByIic(String iic);

    boolean existsByIic(String iic);

    Page<Invoice> findAllByOrderByDateTimeDesc(Pageable pageable);

    @EntityGraph(attributePaths = "items")
    List<Invoice> findAllByOrderByDateTimeDesc();
}
