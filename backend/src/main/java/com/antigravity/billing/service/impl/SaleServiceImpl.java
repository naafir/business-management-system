package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.dto.sale.*;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.repository.CustomerRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.repository.SaleRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.GstCalculationService;
import com.antigravity.billing.service.InventoryService;
import com.antigravity.billing.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final GstCalculationService gstCalculationService;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    @Override
    @Transactional
    public SaleResponseDto createSale(CreateSaleRequest request, UUID userId, String username) {
        Customer customer = null;
        String customerName;
        String customerGstin = request.getCustomerGstin();
        String placeOfSupplyState;
        String placeOfSupplyCode;

        BusinessSettings settings = businessSettingsRepository.findAll().stream().findFirst().orElse(null);
        String businessStateCode = settings != null && settings.getStateCode() != null ? settings.getStateCode() : "27";
        String businessStateName = settings != null && settings.getStateName() != null ? settings.getStateName() : "Maharashtra";

        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));
            if (!customer.isActive()) {
                throw new ApiException("Customer '" + customer.getName() + "' is inactive", HttpStatus.BAD_REQUEST);
            }
            customerName = customer.getName();
            if (customerGstin == null || customerGstin.trim().isEmpty()) {
                customerGstin = customer.getGstin();
            }
            placeOfSupplyState = request.getPlaceOfSupplyState() != null ? request.getPlaceOfSupplyState() : customer.getStateName();
            placeOfSupplyCode = request.getPlaceOfSupplyCode() != null ? request.getPlaceOfSupplyCode() : customer.getStateCode();
        } else {
            customerName = (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty())
                    ? request.getCustomerName().trim()
                    : "Walk-in Customer";
            placeOfSupplyState = request.getPlaceOfSupplyState() != null ? request.getPlaceOfSupplyState() : businessStateName;
            placeOfSupplyCode = request.getPlaceOfSupplyCode() != null ? request.getPlaceOfSupplyCode() : businessStateCode;
        }

        List<GstCalculationRequest.GstLineItemInput> gstItems = new ArrayList<>();
        Map<UUID, Product> productMap = new LinkedHashMap<>();
        Map<UUID, BigDecimal> unitPriceMap = new LinkedHashMap<>();

        for (SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdWithPessimisticLock(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            if (!product.isActive()) {
                throw new ApiException("Product '" + product.getName() + "' is inactive", HttpStatus.BAD_REQUEST);
            }

            if (!product.isAllowNegativeStock() && product.getCurrentStock().compareTo(itemReq.getQuantity()) < 0) {
                throw new ApiException(
                        String.format("Insufficient stock for product '%s'. Available: %s, Requested: %s",
                                product.getName(), product.getCurrentStock(), itemReq.getQuantity()),
                        HttpStatus.BAD_REQUEST
                );
            }

            BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : product.getSellingPrice();
            unitPriceMap.put(product.getId(), unitPrice);

            BigDecimal gstRatePercent = product.getGstRate() != null ? product.getGstRate().getRatePercent() : BigDecimal.ZERO;
            BigDecimal gross = itemReq.getQuantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountPercent = itemReq.getDiscountPercent() != null ? itemReq.getDiscountPercent() : BigDecimal.ZERO;
            BigDecimal discountAmount = gross.multiply(discountPercent).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

            gstItems.add(GstCalculationRequest.GstLineItemInput.builder()
                    .description(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .discount(discountAmount)
                    .gstRatePercent(gstRatePercent)
                    .build());

            productMap.put(itemReq.getProductId(), product);
        }

        GstCalculationResult gstResult = gstCalculationService.calculate(GstCalculationRequest.builder()
                .businessStateCode(businessStateCode)
                .placeOfSupplyStateCode(placeOfSupplyCode)
                .items(gstItems)
                .build());

        BigDecimal grandTotal = gstResult.getGrandTotal();
        BigDecimal amountPaid = request.getAmountPaid() != null ? request.getAmountPaid() : BigDecimal.ZERO;

        // If walk-in customer and payment method is provided but amountPaid wasn't explicitly set, default to grandTotal
        if (customer == null && amountPaid.compareTo(BigDecimal.ZERO) == 0 && request.getPaymentMethod() != null) {
            amountPaid = grandTotal;
        }

        BigDecimal balanceDue = grandTotal.subtract(amountPaid);
        PaymentStatus status;
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            status = PaymentStatus.PAID;
            balanceDue = BigDecimal.ZERO;
        } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            status = PaymentStatus.PARTIALLY_PAID;
        } else {
            status = PaymentStatus.PENDING;
        }

        // Credit limit check
        if (customer != null && customer.getCreditLimit() != null && customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalProjectedDebt = customer.getOutstandingBalance().add(balanceDue);
            if (totalProjectedDebt.compareTo(customer.getCreditLimit()) > 0) {
                throw new ApiException(
                        String.format("Credit limit of %s exceeded for customer '%s'. Current balance: %s, Order balance: %s",
                                customer.getCreditLimit(), customer.getName(), customer.getOutstandingBalance(), balanceDue),
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        String saleNumber = generateUniqueSaleNumber(settings);

        Sale sale = Sale.builder()
                .saleNumber(saleNumber)
                .customer(customer)
                .customerName(customerName)
                .customerGstin(customerGstin)
                .placeOfSupplyState(placeOfSupplyState)
                .placeOfSupplyCode(placeOfSupplyCode)
                .saleDate(request.getSaleDate())
                .subtotal(gstResult.getTotalGross())
                .taxableAmount(gstResult.getTotalTaxableValue())
                .cgstAmount(gstResult.getTotalCgst())
                .sgstAmount(gstResult.getTotalSgst())
                .igstAmount(gstResult.getTotalIgst())
                .totalTax(gstResult.getTotalTax())
                .totalDiscount(gstResult.getTotalDiscount())
                .grandTotal(grandTotal)
                .amountPaid(amountPaid)
                .balanceDue(balanceDue)
                .paymentStatus(status)
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .createdBy(userId)
                .createdByName(username)
                .build();

        for (int i = 0; i < request.getItems().size(); i++) {
            SaleItemRequest itemReq = request.getItems().get(i);
            GstCalculationResult.GstLineItemResult lineRes = gstResult.getItems().get(i);
            Product product = productMap.get(itemReq.getProductId());
            BigDecimal unitPrice = unitPriceMap.get(product.getId());

            SaleItem item = SaleItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .hsnSac(product.getHsnSac())
                    .unit(product.getUnit())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .discountPercent(itemReq.getDiscountPercent() != null ? itemReq.getDiscountPercent() : BigDecimal.ZERO)
                    .discountAmount(lineRes.getDiscount())
                    .taxableAmount(lineRes.getTaxableValue())
                    .gstRatePercent(lineRes.getGstRatePercent())
                    .cgstAmount(lineRes.getCgstAmount())
                    .sgstAmount(lineRes.getSgstAmount())
                    .igstAmount(lineRes.getIgstAmount())
                    .totalAmount(lineRes.getLineTotal())
                    .build();
            sale.addItem(item);
        }

        Sale saved = saleRepository.save(sale);

        // Deduct product stock and record inventory transactions
        for (SaleItem item : saved.getItems()) {
            Product product = item.getProduct();
            BigDecimal prevStock = product.getCurrentStock();
            BigDecimal newStock = prevStock.subtract(item.getQuantity());
            product.setCurrentStock(newStock);
            productRepository.save(product);

            inventoryService.recordTransaction(
                    product,
                    InventoryTransactionType.SALE,
                    item.getQuantity(),
                    prevStock,
                    newStock,
                    product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO,
                    "SALE",
                    saved.getId(),
                    saved.getSaleNumber(),
                    "Sale to " + customerName,
                    userId,
                    username
            );
        }

        // Update customer balance if balance due exists
        if (customer != null && balanceDue.compareTo(BigDecimal.ZERO) > 0) {
            customer.setOutstandingBalance(customer.getOutstandingBalance().add(balanceDue));
            customerRepository.save(customer);
        }

        auditService.logAction(
                userId,
                username,
                "CREATE_SALE",
                "Sale",
                saved.getId().toString(),
                "Created sale invoice: " + saved.getSaleNumber() + " for " + customerName + " total: " + saved.getGrandTotal()
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public SaleResponseDto recordPayment(UUID id, com.antigravity.billing.dto.payment.RecordPaymentRequest request, UUID userId, String username) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));

        BigDecimal paymentAmount = request.getAmountPaid().setScale(2, RoundingMode.HALF_UP);
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Payment amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal currentBalance = sale.getBalanceDue();
        if (paymentAmount.compareTo(currentBalance.add(new BigDecimal("0.01"))) > 0) {
            throw new ApiException("Payment amount ₹" + paymentAmount + " exceeds balance due of ₹" + currentBalance, HttpStatus.BAD_REQUEST);
        }

        BigDecimal newAmountPaid = sale.getAmountPaid().add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalanceDue = sale.getGrandTotal().subtract(newAmountPaid).setScale(2, RoundingMode.HALF_UP);

        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            newBalanceDue = BigDecimal.ZERO;
            sale.setPaymentStatus(PaymentStatus.PAID);
        } else {
            sale.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        sale.setAmountPaid(newAmountPaid);
        sale.setBalanceDue(newBalanceDue);
        if (request.getPaymentMethod() != null) {
            sale.setPaymentMethod(request.getPaymentMethod());
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = sale.getNotes() != null ? sale.getNotes() + " | " : "";
            sale.setNotes(existingNotes + "Payment: ₹" + paymentAmount + " (" + request.getPaymentMethod() + ") " + request.getNotes());
        }

        Sale updatedSale = saleRepository.save(sale);

        // Update customer balance if linked
        if (sale.getCustomer() != null) {
            Customer customer = sale.getCustomer();
            BigDecimal customerBal = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
            BigDecimal newCustBal = customerBal.subtract(paymentAmount);
            if (newCustBal.compareTo(BigDecimal.ZERO) < 0) {
                newCustBal = BigDecimal.ZERO;
            }
            customer.setOutstandingBalance(newCustBal);
            customerRepository.save(customer);
        }

        auditService.logAction(
                userId,
                username,
                "RECORD_PAYMENT",
                "SALE",
                sale.getId().toString(),
                "Recorded payment of ₹" + paymentAmount + " via " + request.getPaymentMethod() + " for Sale #" + sale.getSaleNumber() + ". Remaining balance: ₹" + newBalanceDue
        );

        return mapToDto(updatedSale);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDto getSaleById(UUID id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));
        return mapToDto(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDto getSaleByNumber(String saleNumber) {
        Sale sale = saleRepository.findBySaleNumber(saleNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "saleNumber", saleNumber));
        return mapToDto(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SaleResponseDto> listSales(
            String search,
            UUID customerId,
            PaymentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Sale> page = saleRepository.findSalesWithFilters(searchTerm, customerId, status, startDate, endDate, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public SaleSummaryDto getSaleSummary() {
        BigDecimal totalSales = saleRepository.sumTotalSales();
        BigDecimal totalReceivable = saleRepository.sumTotalReceivableBalance();
        long totalCount = saleRepository.count();

        return SaleSummaryDto.builder()
                .totalSalesAmount(totalSales)
                .totalOutstandingReceivable(totalReceivable)
                .totalSalesCount(totalCount)
                .pendingSalesCount(0)
                .build();
    }

    private synchronized String generateUniqueSaleNumber(BusinessSettings settings) {
        String prefix = (settings != null && settings.getInvoicePrefix() != null)
                ? settings.getInvoicePrefix()
                : "INV-" + LocalDate.now().getYear() + "-";

        long seq = 1;
        if (settings != null && settings.getInvoiceNextSeq() != null) {
            seq = settings.getInvoiceNextSeq();
            settings.setInvoiceNextSeq(seq + 1);
            businessSettingsRepository.save(settings);
        } else {
            seq = saleRepository.count() + 1;
        }

        String candidate = String.format("%s%04d", prefix, seq);
        int offset = 0;
        while (saleRepository.existsBySaleNumber(candidate)) {
            offset++;
            candidate = String.format("%s%04d", prefix, seq + offset);
        }
        return candidate;
    }

    private SaleResponseDto mapToDto(Sale s) {
        List<SaleItemResponseDto> itemDtos = s.getItems().stream()
                .map(item -> SaleItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .hsnSac(item.getHsnSac())
                        .unit(item.getUnit())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discountPercent(item.getDiscountPercent())
                        .discountAmount(item.getDiscountAmount())
                        .taxableAmount(item.getTaxableAmount())
                        .gstRatePercent(item.getGstRatePercent())
                        .cgstAmount(item.getCgstAmount())
                        .sgstAmount(item.getSgstAmount())
                        .igstAmount(item.getIgstAmount())
                        .totalAmount(item.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return SaleResponseDto.builder()
                .id(s.getId())
                .saleNumber(s.getSaleNumber())
                .customerId(s.getCustomer() != null ? s.getCustomer().getId() : null)
                .customerName(s.getCustomerName())
                .customerGstin(s.getCustomerGstin())
                .placeOfSupplyState(s.getPlaceOfSupplyState())
                .placeOfSupplyCode(s.getPlaceOfSupplyCode())
                .saleDate(s.getSaleDate())
                .subtotal(s.getSubtotal())
                .taxableAmount(s.getTaxableAmount())
                .cgstAmount(s.getCgstAmount())
                .sgstAmount(s.getSgstAmount())
                .igstAmount(s.getIgstAmount())
                .totalTax(s.getTotalTax())
                .totalDiscount(s.getTotalDiscount())
                .grandTotal(s.getGrandTotal())
                .amountPaid(s.getAmountPaid())
                .balanceDue(s.getBalanceDue())
                .paymentStatus(s.getPaymentStatus())
                .paymentMethod(s.getPaymentMethod())
                .notes(s.getNotes())
                .createdByName(s.getCreatedByName())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .items(itemDtos)
                .build();
    }
}
