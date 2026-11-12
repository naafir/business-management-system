package com.antigravity.billing.dto.payment;

import com.antigravity.billing.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private UUID id;
    private String paymentNumber;
    private UUID invoiceId;
    private String invoiceNumber;
    private UUID saleId;
    private String saleNumber;
    private UUID customerId;
    private String customerName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private ZonedDateTime paymentDate;
    private String referenceNumber;
    private String notes;
    private UUID createdBy;
    private String createdByName;
    private ZonedDateTime createdAt;
}
