package com.antigravity.billing.repository;

import com.antigravity.billing.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") UUID id);

    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.hsnSac) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:activeOnly IS NULL OR p.active = :activeOnly)")
    Page<Product> findProductsWithFilters(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            @Param("activeOnly") Boolean activeOnly,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.currentStock <= p.minStockLevel")
    long countLowStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.currentStock <= 0")
    long countOutOfStockProducts();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentStock <= p.minStockLevel ORDER BY p.currentStock ASC")
    Page<Product> findLowStockProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentStock <= 0 ORDER BY p.name ASC")
    Page<Product> findOutOfStockProducts(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.currentStock * p.purchasePrice), 0) FROM Product p WHERE p.active = true")
    BigDecimal calculateTotalInventoryValuation();

    @Query("SELECT COALESCE(SUM(p.currentStock), 0) FROM Product p WHERE p.active = true")
    BigDecimal calculateTotalUnitsInStock();
}