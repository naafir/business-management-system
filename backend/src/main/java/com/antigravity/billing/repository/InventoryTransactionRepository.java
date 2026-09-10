package com.antigravity.billing.repository;

import com.antigravity.billing.entity.InventoryTransaction;
import com.antigravity.billing.entity.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    @Query(value = "SELECT it FROM InventoryTransaction it JOIN FETCH it.product p " +
            "WHERE (:productId IS NULL OR p.id = :productId) " +
            "AND (:type IS NULL OR it.transactionType = :type) " +
            "AND (:fromDate IS NULL OR it.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR it.createdAt <= :toDate) " +
            "ORDER BY it.createdAt DESC",
            countQuery = "SELECT count(it) FROM InventoryTransaction it " +
                    "WHERE (:productId IS NULL OR it.product.id = :productId) " +
                    "AND (:type IS NULL OR it.transactionType = :type) " +
                    "AND (:fromDate IS NULL OR it.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR it.createdAt <= :toDate)")
    Page<InventoryTransaction> findWithFilters(
            @Param("productId") UUID productId,
            @Param("type") InventoryTransactionType type,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    @Query(value = "SELECT it FROM InventoryTransaction it JOIN FETCH it.product p " +
            "WHERE p.id = :productId " +
            "ORDER BY it.createdAt DESC",
            countQuery = "SELECT count(it) FROM InventoryTransaction it WHERE it.product.id = :productId")
    Page<InventoryTransaction> findByProductId(
            @Param("productId") UUID productId,
            Pageable pageable
    );

    List<InventoryTransaction> findTop10ByOrderByCreatedAtDesc();
}
