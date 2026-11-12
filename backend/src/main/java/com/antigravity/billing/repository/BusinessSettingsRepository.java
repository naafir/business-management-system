package com.antigravity.billing.repository;

import com.antigravity.billing.entity.BusinessSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BusinessSettings b")
    Optional<BusinessSettings> findFirstForUpdate();
}
