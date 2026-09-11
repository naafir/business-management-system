package com.antigravity.billing.repository;

import com.antigravity.billing.entity.PaymentStatus;
import com.antigravity.billing.entity.Sale;
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
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    Optional<Sale> findBySaleNumber(String saleNumber);

    boolean existsBySaleNumber(String saleNumber);

    @Query("SELECT s FROM Sale s LEFT JOIN s.customer c WHERE " +
           "(:search IS NULL OR LOWER(s.saleNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(s.customerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(s.customerGstin) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:customerId IS NULL OR c.id = :customerId) AND " +
           "(:status IS NULL OR s.paymentStatus = :status) AND " +
           "(:startDate IS NULL OR s.saleDate >= :startDate) AND " +
           "(:endDate IS NULL OR s.saleDate <= :endDate)")
    Page<Sale> findSalesWithFilters(
            @Param("search") String search,
            @Param("customerId") UUID customerId,
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s")
    BigDecimal sumTotalSales();

    @Query("SELECT COALESCE(SUM(s.balanceDue), 0) FROM Sale s")
    BigDecimal sumTotalReceivableBalance();

    long count();
}
