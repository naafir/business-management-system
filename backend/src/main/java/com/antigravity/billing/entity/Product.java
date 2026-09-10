package com.antigravity.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "hsn_sac", nullable = false, length = 20)
    private String hsnSac;

    @Column(name = "unit", nullable = false, length = 20)
    @Builder.Default
    private String unit = "PCS";

    @Column(name = "purchase_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gst_rate_id")
    private GstRate gstRate;

    @Column(name = "opening_stock", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal openingStock = BigDecimal.ZERO;

    @Column(name = "current_stock", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "min_stock_level", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minStockLevel = new BigDecimal("5.00");

    @Column(name = "allow_negative_stock", nullable = false)
    @Builder.Default
    private boolean allowNegativeStock = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}