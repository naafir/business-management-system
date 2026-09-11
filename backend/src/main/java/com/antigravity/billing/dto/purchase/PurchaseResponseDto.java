package com.antigravity.billing.dto.purchase;

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
public class PurchaseResponseDto {
    private UUID id;
    private String purchaseNumber;
    private UUID supplierId;
    private String supplierName;
    private String supplierGstin;
    private String supplierInvoiceNumber;
    private LocalDate purchaseDate;
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
    private List<PurchaseItemResponseDto> items;
}
