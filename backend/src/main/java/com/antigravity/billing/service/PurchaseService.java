package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.purchase.CreatePurchaseRequest;
import com.antigravity.billing.dto.purchase.PurchaseResponseDto;
import com.antigravity.billing.dto.purchase.PurchaseSummaryDto;
import com.antigravity.billing.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface PurchaseService {

    PurchaseResponseDto createPurchase(CreatePurchaseRequest request, UUID userId, String username);

    PurchaseResponseDto recordPayment(UUID id, com.antigravity.billing.dto.payment.RecordPaymentRequest request, UUID userId, String username);

    PurchaseResponseDto getPurchaseById(UUID id);

    PurchaseResponseDto getPurchaseByNumber(String purchaseNumber);

    PageResponse<PurchaseResponseDto> listPurchases(
            String search,
            UUID supplierId,
            PaymentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    PurchaseSummaryDto getPurchaseSummary();
}
