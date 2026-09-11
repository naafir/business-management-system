package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.sale.CreateSaleRequest;
import com.antigravity.billing.dto.sale.SaleResponseDto;
import com.antigravity.billing.dto.sale.SaleSummaryDto;
import com.antigravity.billing.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface SaleService {

    SaleResponseDto createSale(CreateSaleRequest request, UUID userId, String username);

    SaleResponseDto getSaleById(UUID id);

    SaleResponseDto getSaleByNumber(String saleNumber);

    PageResponse<SaleResponseDto> listSales(
            String search,
            UUID customerId,
            PaymentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    SaleSummaryDto getSaleSummary();
}
