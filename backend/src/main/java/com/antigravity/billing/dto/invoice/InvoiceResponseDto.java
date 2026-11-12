package com.antigravity.billing.dto.invoice;

import com.antigravity.billing.entity.InvoiceStatus;
import com.antigravity.billing.entity.PaymentMethod;
import com.antigravity.billing.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDto {
    private UUID id;
    private String invoiceNumber;
    private UUID saleId;
    private UUID customerId;
    private String customerName;
    private String customerGstin;
    private String placeOfSupplyState;
    private String placeOfSupplyCode;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
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
    private String termsAndConditions;
    private String sellerLegalName;
    private String sellerTradeName;
    private String sellerGstin;
    private String sellerAddressLine1;
    private String sellerAddressLine2;
    private String sellerCity;
    private String sellerStateName;
    private String sellerStateCode;
    private String sellerPinCode;
    private String sellerPhone;
    private String sellerEmail;
    private String sellerBankName;
    private String sellerBankAccountNumber;
    private String sellerBankIfsc;
    private String sellerBankBranch;
    private String sellerBankUpiId;
    private String createdByName;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private List<InvoiceItemDto> items;
}
