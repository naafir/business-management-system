package com.antigravity.billing.dto.sale;

import com.antigravity.billing.entity.PaymentMethod;
import com.antigravity.billing.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponseDto {
    private UUID id;
    private String saleNumber;
    private UUID customerId;
    private String customerName;
    private String customerGstin;
    private String placeOfSupplyState;
    private String placeOfSupplyCode;
    private LocalDate saleDate;
    private BigDecimal subtotal;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalTax;
    private BigDecimal totalDiscount;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String notes;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SaleItemResponseDto> items;
}
