package com.antigravity.billing.repository;

import com.antigravity.billing.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("SELECT e FROM Expense e LEFT JOIN e.category c WHERE " +
           "(:search IS NULL OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(e.vendorName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR c.id = :categoryId) AND " +
           "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
           "(:endDate IS NULL OR e.expenseDate <= :endDate)")
    Page<Expense> findExpensesWithFilters(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e")
    BigDecimal sumTotalExpenses();

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.expenseDate >= :startDate AND e.expenseDate <= :endDate")
    BigDecimal sumExpensesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.gstAmount), 0) FROM Expense e WHERE e.gstEligible = TRUE AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate")
    BigDecimal sumEligibleGstByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long count();
}
