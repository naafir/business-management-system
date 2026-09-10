package com.antigravity.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_name", nullable = false, length = 100)
    private String stateName;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}