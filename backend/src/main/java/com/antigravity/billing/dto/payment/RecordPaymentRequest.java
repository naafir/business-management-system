package com.antigravity.billing.dto.payment;

import com.antigravity.billing.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordPaymentRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @jakarta.validation.constraints.Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    @jakarta.validation.constraints.Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public BigDecimal getAmount() {
        return amountPaid;
    }

    public void setAmount(BigDecimal amount) {
        this.amountPaid = amount;
    }
}
