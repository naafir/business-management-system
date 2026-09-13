package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.dto.invoice.*;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.*;
import com.antigravity.billing.service.GstCalculationService;
import com.antigravity.billing.service.InvoiceService;
import com.antigravity.billing.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final GstCalculationService gstCalculationService;
    private final PdfGeneratorService pdfGeneratorService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponseDto> listInvoices(
            String search,
            UUID customerId,
            InvoiceStatus status,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Page<Invoice> page = invoiceRepository.searchInvoices(
                search, customerId, status, paymentStatus, startDate, endDate, pageable
        );
        List<InvoiceResponseDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PageResponse.<InvoiceResponseDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceSummaryDto getInvoiceSummary() {
        BigDecimal totalInvoiced = invoiceRepository.sumTotalInvoiced();
        BigDecimal totalOutstanding = invoiceRepository.sumTotalOutstanding();
        long totalCount = invoiceRepository.count();
        long finalizedCount = invoiceRepository.countByStatus(InvoiceStatus.FINALIZED);
        long draftCount = invoiceRepository.countByStatus(InvoiceStatus.DRAFT);

        return InvoiceSummaryDto.builder()
                .totalInvoicedAmount(totalInvoiced)
                .totalOutstandingReceivable(totalOutstanding)
                .totalInvoicesCount(totalCount)
                .finalizedInvoicesCount(finalizedCount)
                .draftInvoicesCount(draftCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceById(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return mapToDto(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "number", invoiceNumber));
        return mapToDto(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponseDto createInvoice(CreateInvoiceRequest request, UUID userId, String username) {
        BusinessSettings settings = businessSettingsRepository.findAll().stream().findFirst().orElse(null);
        String businessStateCode = settings != null && settings.getStateCode() != null ? settings.getStateCode() : "27";

        String placeOfSupplyCode = request.getPlaceOfSupplyCode() != null ? request.getPlaceOfSupplyCode() : businessStateCode;
        String placeOfSupplyState = request.getPlaceOfSupplyState() != null ? request.getPlaceOfSupplyState() : "Maharashtra";

        String customerName = "Walk-in Customer";
        String customerGstin = request.getCustomerGstin();

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));
            customerName = customer.getName();
            customerGstin = customer.getGstin();
        } else if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            customerName = request.getCustomerName().trim();
        }

        String invoiceNumber = generateNextInvoiceNumber(settings);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .saleId(request.getSaleId())
                .customerId(request.getCustomerId())
                .customerName(customerName)
                .customerGstin(customerGstin)
                .placeOfSupplyState(placeOfSupplyState)
                .placeOfSupplyCode(placeOfSupplyCode)
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : request.getInvoiceDate().plusDays(30))
                .status(InvoiceStatus.DRAFT)
                .notes(request.getNotes())
                .termsAndConditions(request.getTermsAndConditions() != null ? request.getTermsAndConditions() : (settings != null ? settings.getInvoiceTerms() : null))
                .createdBy(userId)
                .createdByName(username)
                .build();

        List<GstCalculationRequest.GstLineItemInput> gstItems = new ArrayList<>();
        List<TempItemData> tempDataList = new ArrayList<>();

        for (InvoiceItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : product.getSellingPrice();
            BigDecimal discountPercent = itemReq.getDiscountPercent() != null ? itemReq.getDiscountPercent() : BigDecimal.ZERO;

            BigDecimal grossAmount = unitPrice.multiply(itemReq.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountAmount = grossAmount.multiply(discountPercent).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

            BigDecimal gstRate = (product.getGstRate() != null && product.getGstRate().getRatePercent() != null)
                    ? product.getGstRate().getRatePercent()
                    : BigDecimal.ZERO;

            gstItems.add(GstCalculationRequest.GstLineItemInput.builder()
                    .description(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .discount(discountAmount)
                    .gstRatePercent(gstRate)
                    .build());

            tempDataList.add(new TempItemData(product, unitPrice, discountPercent, discountAmount));
        }

        GstCalculationResult gstResult = gstCalculationService.calculate(GstCalculationRequest.builder()
                .businessStateCode(businessStateCode)
                .placeOfSupplyStateCode(placeOfSupplyCode)
                .items(gstItems)
                .build());

        for (int i = 0; i < tempDataList.size(); i++) {
            TempItemData td = tempDataList.get(i);
            GstCalculationResult.GstLineItemResult lineRes = gstResult.getItems().get(i);

            InvoiceItem item = InvoiceItem.builder()
                    .productId(td.product.getId())
                    .productName(td.product.getName())
                    .productSku(td.product.getSku())
                    .hsnSac(td.product.getHsnSac())
                    .unit(td.product.getUnit())
                    .quantity(lineRes.getQuantity())
                    .unitPrice(td.unitPrice)
                    .discountPercent(td.discountPercent)
                    .discountAmount(td.discountAmount)
                    .taxableAmount(lineRes.getTaxableValue())
                    .gstRatePercent(lineRes.getGstRatePercent())
                    .cgstAmount(lineRes.getCgstAmount())
                    .sgstAmount(lineRes.getSgstAmount())
                    .igstAmount(lineRes.getIgstAmount())
                    .totalAmount(lineRes.getLineTotal())
                    .build();

            invoice.addItem(item);
        }

        BigDecimal grandTotal = gstResult.getGrandTotal();
        BigDecimal amountPaid = request.getAmountPaid() != null ? request.getAmountPaid() : grandTotal;
        BigDecimal balanceDue = grandTotal.subtract(amountPaid).setScale(2, RoundingMode.HALF_UP);

        PaymentStatus paymentStatus = PaymentStatus.PAID;
        if (balanceDue.compareTo(BigDecimal.ZERO) > 0) {
            paymentStatus = amountPaid.compareTo(BigDecimal.ZERO) > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PENDING;
        }

        invoice.setSubtotal(gstResult.getTotalGross());
        invoice.setTotalDiscount(gstResult.getTotalDiscount());
        invoice.setTaxableAmount(gstResult.getTotalTaxableValue());
        invoice.setCgstAmount(gstResult.getTotalCgst());
        invoice.setSgstAmount(gstResult.getTotalSgst());
        invoice.setIgstAmount(gstResult.getTotalIgst());
        invoice.setTotalTax(gstResult.getTotalTax());
        invoice.setGrandTotal(grandTotal);
        invoice.setAmountPaid(amountPaid);
        invoice.setBalanceDue(balanceDue);
        invoice.setPaymentStatus(paymentStatus);
        invoice.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created Invoice #{} for customer {}", saved.getInvoiceNumber(), saved.getCustomerName());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public InvoiceResponseDto createInvoiceFromSale(UUID saleId, UUID userId, String username) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", saleId));

        invoiceRepository.findBySaleId(saleId).ifPresent(inv -> {
            throw new IllegalStateException("Invoice already exists for sale number: " + sale.getSaleNumber());
        });

        BusinessSettings settings = businessSettingsRepository.findAll().stream().findFirst().orElse(null);
        String invoiceNumber = generateNextInvoiceNumber(settings);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .saleId(sale.getId())
                .customerId(sale.getCustomer() != null ? sale.getCustomer().getId() : null)
                .customerName(sale.getCustomerName())
                .customerGstin(sale.getCustomerGstin())
                .placeOfSupplyState(sale.getPlaceOfSupplyState())
                .placeOfSupplyCode(sale.getPlaceOfSupplyCode())
                .invoiceDate(sale.getSaleDate())
                .dueDate(sale.getSaleDate().plusDays(30))
                .status(InvoiceStatus.FINALIZED)
                .subtotal(sale.getSubtotal())
                .taxableAmount(sale.getTaxableAmount())
                .cgstAmount(sale.getCgstAmount())
                .sgstAmount(sale.getSgstAmount())
                .igstAmount(sale.getIgstAmount())
                .totalTax(sale.getTotalTax())
                .totalDiscount(sale.getTotalDiscount())
                .grandTotal(sale.getGrandTotal())
                .amountPaid(sale.getAmountPaid())
                .balanceDue(sale.getBalanceDue())
                .paymentStatus(sale.getPaymentStatus())
                .paymentMethod(sale.getPaymentMethod())
                .notes(sale.getNotes())
                .termsAndConditions(settings != null ? settings.getInvoiceTerms() : null)
                .createdBy(userId)
                .createdByName(username)
                .build();

        if (sale.getItems() != null) {
            for (SaleItem sItem : sale.getItems()) {
                InvoiceItem iItem = InvoiceItem.builder()
                        .productId(sItem.getProduct() != null ? sItem.getProduct().getId() : null)
                        .productName(sItem.getProductName())
                        .productSku(sItem.getProductSku())
                        .hsnSac(sItem.getHsnSac())
                        .unit(sItem.getUnit())
                        .quantity(sItem.getQuantity())
                        .unitPrice(sItem.getUnitPrice())
                        .discountPercent(sItem.getDiscountPercent())
                        .discountAmount(sItem.getDiscountAmount())
                        .taxableAmount(sItem.getTaxableAmount())
                        .gstRatePercent(sItem.getGstRatePercent())
                        .cgstAmount(sItem.getCgstAmount())
                        .sgstAmount(sItem.getSgstAmount())
                        .igstAmount(sItem.getIgstAmount())
                        .totalAmount(sItem.getTotalAmount())
                        .build();

                invoice.addItem(iItem);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Generated Invoice #{} from Sale #{}", saved.getInvoiceNumber(), sale.getSaleNumber());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public InvoiceResponseDto finalizeInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.FINALIZED) {
            return mapToDto(invoice);
        }

        invoice.setStatus(InvoiceStatus.FINALIZED);
        Invoice updated = invoiceRepository.save(invoice);
        log.info("Finalized Invoice #{}", updated.getInvoiceNumber());

        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadInvoicePdf(UUID id) {
        InvoiceResponseDto invoiceDto = getInvoiceById(id);
        return pdfGeneratorService.generateInvoicePdf(invoiceDto);
    }

    private synchronized String generateNextInvoiceNumber(BusinessSettings settings) {
        String prefix = settings != null && settings.getInvoicePrefix() != null ? settings.getInvoicePrefix() : "INV-";
        long nextSeq = settings != null ? settings.getInvoiceNextSeq() : 1;

        if (settings != null) {
            settings.setInvoiceNextSeq(nextSeq + 1);
            businessSettingsRepository.save(settings);
        }

        String yearStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        return String.format("%s%s-%05d", prefix, yearStr, nextSeq);
    }

    private InvoiceResponseDto mapToDto(Invoice invoice) {
        List<InvoiceItemDto> itemDtos = invoice.getItems() != null ?
                invoice.getItems().stream().map(i -> InvoiceItemDto.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .productSku(i.getProductSku())
                        .hsnSac(i.getHsnSac())
                        .unit(i.getUnit())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .discountPercent(i.getDiscountPercent())
                        .discountAmount(i.getDiscountAmount())
                        .taxableAmount(i.getTaxableAmount())
                        .gstRatePercent(i.getGstRatePercent())
                        .cgstAmount(i.getCgstAmount())
                        .sgstAmount(i.getSgstAmount())
                        .igstAmount(i.getIgstAmount())
                        .totalAmount(i.getTotalAmount())
                        .build()
                ).collect(Collectors.toList()) : new ArrayList<>();

        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .saleId(invoice.getSaleId())
                .customerId(invoice.getCustomerId())
                .customerName(invoice.getCustomerName())
                .customerGstin(invoice.getCustomerGstin())
                .placeOfSupplyState(invoice.getPlaceOfSupplyState())
                .placeOfSupplyCode(invoice.getPlaceOfSupplyCode())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .subtotal(invoice.getSubtotal())
                .taxableAmount(invoice.getTaxableAmount())
                .cgstAmount(invoice.getCgstAmount())
                .sgstAmount(invoice.getSgstAmount())
                .igstAmount(invoice.getIgstAmount())
                .totalTax(invoice.getTotalTax())
                .totalDiscount(invoice.getTotalDiscount())
                .grandTotal(invoice.getGrandTotal())
                .amountPaid(invoice.getAmountPaid())
                .balanceDue(invoice.getBalanceDue())
                .paymentStatus(invoice.getPaymentStatus())
                .paymentMethod(invoice.getPaymentMethod())
                .notes(invoice.getNotes())
                .termsAndConditions(invoice.getTermsAndConditions())
                .createdByName(invoice.getCreatedByName())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(itemDtos)
                .build();
    }

    private static class TempItemData {
        Product product;
        BigDecimal unitPrice;
        BigDecimal discountPercent;
        BigDecimal discountAmount;

        TempItemData(Product product, BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal discountAmount) {
            this.product = product;
            this.unitPrice = unitPrice;
            this.discountPercent = discountPercent;
            this.discountAmount = discountAmount;
        }
    }
}
