package com.antigravity.billing.repository;

import com.antigravity.billing.entity.GstRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GstRateRepository extends JpaRepository<GstRate, UUID> {
    Optional<GstRate> findByRatePercent(BigDecimal ratePercent);
    List<GstRate> findByActiveTrueOrderByRatePercentAsc();
}
