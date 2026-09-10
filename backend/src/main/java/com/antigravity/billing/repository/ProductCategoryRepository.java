package com.antigravity.billing.repository;

import com.antigravity.billing.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    Optional<ProductCategory> findByNameIgnoreCase(String name);
    List<ProductCategory> findByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}