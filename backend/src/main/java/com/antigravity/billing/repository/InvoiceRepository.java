package com.antigravity.billing.repository;

import com.antigravity.billing.entity.Invoice;
import com.antigravity.billing.entity.InvoiceStatus;
import com.antigravity.billing.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findBySaleId(UUID saleId);

    @Query("SELECT i FROM Invoice i WHERE " +
           "(:search IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:customerId IS NULL OR i.customerId = :customerId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:paymentStatus IS NULL OR i.paymentStatus = :paymentStatus) AND " +
           "(:startDate IS NULL OR i.invoiceDate >= :startDate) AND " +
           "(:endDate IS NULL OR i.invoiceDate <= :endDate)")
    Page<Invoice> searchInvoices(
            @Param("search") String search,
            @Param("customerId") UUID customerId,
            @Param("status") InvoiceStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.status != 'CANCELLED'")
    BigDecimal sumTotalInvoiced();

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i WHERE i.status != 'CANCELLED'")
    BigDecimal sumTotalOutstanding();

    long countByStatus(InvoiceStatus status);

    long countByPaymentStatus(PaymentStatus paymentStatus);
}
