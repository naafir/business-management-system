package com.antigravity.billing.repository;

import com.antigravity.billing.entity.PaymentStatus;
import com.antigravity.billing.entity.Purchase;
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
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

    boolean existsByPurchaseNumber(String purchaseNumber);

    @Query("SELECT p FROM Purchase p LEFT JOIN p.supplier s WHERE " +
           "(:search IS NULL OR LOWER(p.purchaseNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(p.supplierInvoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:supplierId IS NULL OR s.id = :supplierId) AND " +
           "(:status IS NULL OR p.paymentStatus = :status) AND " +
           "(:startDate IS NULL OR p.purchaseDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.purchaseDate <= :endDate)")
    Page<Purchase> findPurchasesWithFilters(
            @Param("search") String search,
            @Param("supplierId") UUID supplierId,
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM Purchase p")
    BigDecimal sumTotalPurchases();

    @Query("SELECT COALESCE(SUM(p.balanceDue), 0) FROM Purchase p")
    BigDecimal sumTotalOutstandingBalance();

    long count();
}
