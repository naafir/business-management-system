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

    private String notes;
}
