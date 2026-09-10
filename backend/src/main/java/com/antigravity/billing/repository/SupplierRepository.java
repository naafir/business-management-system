package com.antigravity.billing.repository;

import com.antigravity.billing.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.gstin) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:activeOnly IS NULL OR s.active = :activeOnly)")
    Page<Supplier> findSuppliersWithFilters(
            @Param("search") String search,
            @Param("activeOnly") Boolean activeOnly,
            Pageable pageable
    );
}