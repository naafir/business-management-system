package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.dto.purchase.*;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.repository.PurchaseRepository;
import com.antigravity.billing.repository.SupplierRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.GstCalculationService;
import com.antigravity.billing.service.InventoryService;
import com.antigravity.billing.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final GstCalculationService gstCalculationService;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    @Override
    @Transactional
    public PurchaseResponseDto createPurchase(CreatePurchaseRequest request, UUID userId, String username) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        BusinessSettings settings = businessSettingsRepository.findAll().stream().findFirst().orElse(null);
        String businessStateCode = settings != null && settings.getStateCode() != null ? settings.getStateCode() : "27";
        String supplierStateCode = supplier.getStateCode() != null ? supplier.getStateCode() : businessStateCode;

        List<GstCalculationRequest.GstLineItemInput> gstItems = new ArrayList<>();
        Map<UUID, Product> productMap = new LinkedHashMap<>();

        for (PurchaseItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdWithPessimisticLock(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            BigDecimal gstRatePercent = product.getGstRate() != null ? product.getGstRate().getRatePercent() : BigDecimal.ZERO;
            BigDecimal gross = itemReq.getQuantity().multiply(itemReq.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountPercent = itemReq.getDiscountPercent() != null ? itemReq.getDiscountPercent() : BigDecimal.ZERO;
            BigDecimal discountAmount = gross.multiply(discountPercent).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

            gstItems.add(GstCalculationRequest.GstLineItemInput.builder()
                    .description(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .discount(discountAmount)
                    .gstRatePercent(gstRatePercent)
                    .build());

            productMap.put(itemReq.getProductId(), product);
        }

        GstCalculationResult gstResult = gstCalculationService.calculate(GstCalculationRequest.builder()
                .businessStateCode(businessStateCode)
                .placeOfSupplyStateCode(supplierStateCode)
                .items(gstItems)
                .build());

        String purchaseNumber = generateUniquePurchaseNumber();

        BigDecimal amountPaid = request.getAmountPaid() != null ? request.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal grandTotal = gstResult.getGrandTotal();
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

        Purchase purchase = Purchase.builder()
                .purchaseNumber(purchaseNumber)
                .supplier(supplier)
                .supplierInvoiceNumber(request.getSupplierInvoiceNumber())
                .purchaseDate(request.getPurchaseDate())
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
            PurchaseItemRequest itemReq = request.getItems().get(i);
            GstCalculationResult.GstLineItemResult lineRes = gstResult.getItems().get(i);
            Product product = productMap.get(itemReq.getProductId());

            PurchaseItem item = PurchaseItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .hsnSac(product.getHsnSac())
                    .unit(product.getUnit())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .discountPercent(itemReq.getDiscountPercent() != null ? itemReq.getDiscountPercent() : BigDecimal.ZERO)
                    .discountAmount(lineRes.getDiscount())
                    .taxableAmount(lineRes.getTaxableValue())
                    .gstRatePercent(lineRes.getGstRatePercent())
                    .cgstAmount(lineRes.getCgstAmount())
                    .sgstAmount(lineRes.getSgstAmount())
                    .igstAmount(lineRes.getIgstAmount())
                    .totalAmount(lineRes.getLineTotal())
                    .build();
            purchase.addItem(item);
        }

        Purchase saved = purchaseRepository.save(purchase);

        // Update product stock and inventory ledger
        for (PurchaseItem item : saved.getItems()) {
            Product product = item.getProduct();
            BigDecimal prevStock = product.getCurrentStock();
            BigDecimal newStock = prevStock.add(item.getQuantity());
            product.setCurrentStock(newStock);
            productRepository.save(product);

            inventoryService.recordTransaction(
                    product,
                    InventoryTransactionType.PURCHASE,
                    item.getQuantity(),
                    prevStock,
                    newStock,
                    item.getUnitPrice(),
                    "PURCHASE",
                    saved.getId(),
                    saved.getPurchaseNumber(),
                    "Purchase from " + supplier.getName(),
                    userId,
                    username
            );
        }

        // Update supplier balance if balance due exists
        if (balanceDue.compareTo(BigDecimal.ZERO) > 0) {
            supplier.setOutstandingBalance(supplier.getOutstandingBalance().add(balanceDue));
            supplierRepository.save(supplier);
        }

        auditService.logAction(
                userId,
                username,
                "CREATE_PURCHASE",
                "Purchase",
                saved.getId().toString(),
                "Created purchase: " + saved.getPurchaseNumber() + " from " + supplier.getName() + " for total: " + saved.getGrandTotal()
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public PurchaseResponseDto recordPayment(UUID id, com.antigravity.billing.dto.payment.RecordPaymentRequest request, UUID userId, String username) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));

        BigDecimal paymentAmount = request.getAmountPaid().setScale(2, RoundingMode.HALF_UP);
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Payment amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal currentBalance = purchase.getBalanceDue();
        if (paymentAmount.compareTo(currentBalance.add(new BigDecimal("0.01"))) > 0) {
            throw new ApiException("Payment amount ₹" + paymentAmount + " exceeds balance due of ₹" + currentBalance, HttpStatus.BAD_REQUEST);
        }

        BigDecimal newAmountPaid = purchase.getAmountPaid().add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalanceDue = purchase.getGrandTotal().subtract(newAmountPaid).setScale(2, RoundingMode.HALF_UP);

        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            newBalanceDue = BigDecimal.ZERO;
            purchase.setPaymentStatus(PaymentStatus.PAID);
        } else {
            purchase.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        purchase.setAmountPaid(newAmountPaid);
        purchase.setBalanceDue(newBalanceDue);
        if (request.getPaymentMethod() != null) {
            purchase.setPaymentMethod(request.getPaymentMethod());
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = purchase.getNotes() != null ? purchase.getNotes() + " | " : "";
            purchase.setNotes(existingNotes + "Payment: ₹" + paymentAmount + " (" + request.getPaymentMethod() + ") " + request.getNotes());
        }

        Purchase updatedPurchase = purchaseRepository.save(purchase);

        // Update supplier outstanding balance
        if (purchase.getSupplier() != null) {
            Supplier supplier = purchase.getSupplier();
            BigDecimal supplierBal = supplier.getOutstandingBalance() != null ? supplier.getOutstandingBalance() : BigDecimal.ZERO;
            BigDecimal newSupBal = supplierBal.subtract(paymentAmount);
            if (newSupBal.compareTo(BigDecimal.ZERO) < 0) {
                newSupBal = BigDecimal.ZERO;
            }
            supplier.setOutstandingBalance(newSupBal);
            supplierRepository.save(supplier);
        }

        auditService.logAction(
                userId,
                username,
                "RECORD_PAYMENT",
                "PURCHASE",
                purchase.getId().toString(),
                "Recorded payment of ₹" + paymentAmount + " via " + request.getPaymentMethod() + " for Purchase #" + purchase.getPurchaseNumber() + ". Remaining balance: ₹" + newBalanceDue
        );

        return mapToDto(updatedPurchase);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponseDto getPurchaseById(UUID id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));
        return mapToDto(purchase);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponseDto getPurchaseByNumber(String purchaseNumber) {
        Purchase purchase = purchaseRepository.findByPurchaseNumber(purchaseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "purchaseNumber", purchaseNumber));
        return mapToDto(purchase);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseResponseDto> listPurchases(
            String search,
            UUID supplierId,
            PaymentStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Purchase> page = purchaseRepository.findPurchasesWithFilters(searchTerm, supplierId, status, startDate, endDate, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseSummaryDto getPurchaseSummary() {
        BigDecimal totalAmount = purchaseRepository.sumTotalPurchases();
        BigDecimal totalPayable = purchaseRepository.sumTotalOutstandingBalance();
        long totalCount = purchaseRepository.count();

        return PurchaseSummaryDto.builder()
                .totalPurchasesAmount(totalAmount)
                .totalOutstandingPayable(totalPayable)
                .totalPurchasesCount(totalCount)
                .pendingPurchasesCount(0)
                .build();
    }

    private synchronized String generateUniquePurchaseNumber() {
        long count = purchaseRepository.count() + 1;
        String candidate = String.format("PUR-%d-%04d", LocalDate.now().getYear(), count);
        int offset = 0;
        while (purchaseRepository.existsByPurchaseNumber(candidate)) {
            offset++;
            candidate = String.format("PUR-%d-%04d", LocalDate.now().getYear(), count + offset);
        }
        return candidate;
    }

    private PurchaseResponseDto mapToDto(Purchase p) {
        List<PurchaseItemResponseDto> itemDtos = p.getItems().stream()
                .map(item -> PurchaseItemResponseDto.builder()
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

        return PurchaseResponseDto.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .supplierId(p.getSupplier() != null ? p.getSupplier().getId() : null)
                .supplierName(p.getSupplier() != null ? p.getSupplier().getName() : "")
                .supplierGstin(p.getSupplier() != null ? p.getSupplier().getGstin() : null)
                .supplierInvoiceNumber(p.getSupplierInvoiceNumber())
                .purchaseDate(p.getPurchaseDate())
                .subtotal(p.getSubtotal())
                .taxableAmount(p.getTaxableAmount())
                .cgstAmount(p.getCgstAmount())
                .sgstAmount(p.getSgstAmount())
                .igstAmount(p.getIgstAmount())
                .totalTax(p.getTotalTax())
                .totalDiscount(p.getTotalDiscount())
                .grandTotal(p.getGrandTotal())
                .amountPaid(p.getAmountPaid())
                .balanceDue(p.getBalanceDue())
                .paymentStatus(p.getPaymentStatus())
                .paymentMethod(p.getPaymentMethod())
                .notes(p.getNotes())
                .createdByName(p.getCreatedByName())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .items(itemDtos)
                .build();
    }
}
