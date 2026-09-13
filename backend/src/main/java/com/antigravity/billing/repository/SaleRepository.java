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
import java.util.List;
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

    // Report queries
    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(s.taxableAmount), 0) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    BigDecimal sumTaxableValueByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(s.cgstAmount), 0) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    BigDecimal sumCgstByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(s.sgstAmount), 0) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    BigDecimal sumSgstByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(s.igstAmount), 0) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    BigDecimal sumIgstByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(s.taxableAmount), 0) FROM Sale s WHERE s.customerGstin IS NOT NULL AND s.customerGstin <> '' AND s.saleDate >= :startDate AND s.saleDate <= :endDate")
    BigDecimal sumB2bTaxableValueByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Monthly trend
    @Query("SELECT YEAR(s.saleDate), MONTH(s.saleDate), COALESCE(SUM(s.grandTotal),0), COUNT(s) " +
           "FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate " +
           "GROUP BY YEAR(s.saleDate), MONTH(s.saleDate) ORDER BY YEAR(s.saleDate), MONTH(s.saleDate)")
    List<Object[]> monthlySalesTrend(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // HSN summary
    @Query("SELECT si.hsnSac, COALESCE(SUM(si.quantity),0), COALESCE(SUM(si.taxableAmount),0), " +
           "COALESCE(SUM(si.cgstAmount),0), COALESCE(SUM(si.sgstAmount),0), COALESCE(SUM(si.igstAmount),0), " +
           "COALESCE(SUM(si.totalAmount),0) " +
           "FROM SaleItem si JOIN si.sale s " +
           "WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate " +
           "GROUP BY si.hsnSac ORDER BY si.hsnSac")
    List<Object[]> hsnSummaryByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long count();
}
