package com.antigravity.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gst_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstRate extends BaseEntity {

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2, unique = true)
    private BigDecimal ratePercent;

    @Column(name = "description", nullable = false, length = 100)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    @Builder.Default
    private LocalDate effectiveFrom = LocalDate.now();

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
