package com.antigravity.billing.repository;

import com.antigravity.billing.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:search IS NULL OR LOWER(c.name) LIKE :search OR " +
           "LOWER(c.businessName) LIKE :search OR " +
           "LOWER(c.phone) LIKE :search OR " +
           "LOWER(c.gstin) LIKE :search) AND " +
           "(:customerType IS NULL OR c.customerType = :customerType) AND " +
           "(:activeOnly IS NULL OR c.active = :activeOnly)")
    Page<Customer> findCustomersWithFilters(
            @Param("search") String search,
            @Param("customerType") String customerType,
            @Param("activeOnly") Boolean activeOnly,
            Pageable pageable
    );
}