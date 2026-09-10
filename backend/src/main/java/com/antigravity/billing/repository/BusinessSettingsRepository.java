package com.antigravity.billing.repository;

import com.antigravity.billing.entity.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, UUID> {
}
