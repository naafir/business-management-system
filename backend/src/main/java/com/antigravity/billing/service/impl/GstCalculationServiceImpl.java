package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.dto.gst.GstCalculationResult.GstLineItemResult;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.service.GstCalculationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class GstCalculationServiceImpl implements GstCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TWO_HUNDRED = new BigDecimal("200.00");
    private static final BigDecimal TWO = new BigDecimal("2.00");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public boolean isInterState(String businessStateCode, String placeOfSupplyStateCode) {
        if (businessStateCode == null || placeOfSupplyStateCode == null) {
            throw new ApiException("Both business state code and place of supply state code are required", HttpStatus.BAD_REQUEST);
        }
        return !businessStateCode.trim().equalsIgnoreCase(placeOfSupplyStateCode.trim());
    }

    @Override
    public GstCalculationResult calculate(GstCalculationRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException("At least one line item is required for tax calculation", HttpStatus.BAD_REQUEST);
        }

        boolean interState = isInterState(request.getBusinessStateCode(), request.getPlaceOfSupplyStateCode());

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        List<GstLineItemResult> itemResults = new ArrayList<>();

        for (GstCalculationRequest.GstLineItemInput item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException("Line item quantity must be greater than zero", HttpStatus.BAD_REQUEST);
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException("Line item unit price cannot be negative", HttpStatus.BAD_REQUEST);
            }

            BigDecimal rate = item.getGstRatePercent() != null ? item.getGstRatePercent() : BigDecimal.ZERO;
            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException("GST rate cannot be negative", HttpStatus.BAD_REQUEST);
            }

            BigDecimal discount = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException("Discount cannot be negative", HttpStatus.BAD_REQUEST);
            }

            BigDecimal gross = item.getQuantity().multiply(item.getUnitPrice()).setScale(SCALE, ROUNDING_MODE);

            if (discount.compareTo(gross) > 0) {
                throw new ApiException("Discount cannot exceed line item gross total", HttpStatus.BAD_REQUEST);
            }

            BigDecimal taxable = gross.subtract(discount).setScale(SCALE, ROUNDING_MODE);

            BigDecimal cgstRate = BigDecimal.ZERO;
            BigDecimal cgstAmount = BigDecimal.ZERO;
            BigDecimal sgstRate = BigDecimal.ZERO;
            BigDecimal sgstAmount = BigDecimal.ZERO;
            BigDecimal igstRate = BigDecimal.ZERO;
            BigDecimal igstAmount = BigDecimal.ZERO;

            if (interState) {
                igstRate = rate;
                igstAmount = taxable.multiply(rate).divide(HUNDRED, SCALE, ROUNDING_MODE);
            } else {
                cgstRate = rate.divide(TWO, SCALE, ROUNDING_MODE);
                sgstRate = rate.divide(TWO, SCALE, ROUNDING_MODE);
                cgstAmount = taxable.multiply(rate).divide(TWO_HUNDRED, SCALE, ROUNDING_MODE);
                sgstAmount = taxable.multiply(rate).divide(TWO_HUNDRED, SCALE, ROUNDING_MODE);
            }

            BigDecimal lineTotal = taxable.add(cgstAmount).add(sgstAmount).add(igstAmount).setScale(SCALE, ROUNDING_MODE);

            totalGross = totalGross.add(gross);
            totalDiscount = totalDiscount.add(discount);
            totalTaxable = totalTaxable.add(taxable);
            totalCgst = totalCgst.add(cgstAmount);
            totalSgst = totalSgst.add(sgstAmount);
            totalIgst = totalIgst.add(igstAmount);

            itemResults.add(GstLineItemResult.builder()
                    .description(item.getDescription())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .grossAmount(gross)
                    .discount(discount)
                    .taxableValue(taxable)
                    .gstRatePercent(rate)
                    .cgstRatePercent(cgstRate)
                    .cgstAmount(cgstAmount)
                    .sgstRatePercent(sgstRate)
                    .sgstAmount(sgstAmount)
                    .igstRatePercent(igstRate)
                    .igstAmount(igstAmount)
                    .lineTotal(lineTotal)
                    .build());
        }

        BigDecimal totalTax = totalCgst.add(totalSgst).add(totalIgst).setScale(SCALE, ROUNDING_MODE);
        BigDecimal grandTotal = totalTaxable.add(totalTax).setScale(SCALE, ROUNDING_MODE);

        return GstCalculationResult.builder()
                .isInterState(interState)
                .businessStateCode(request.getBusinessStateCode())
                .placeOfSupplyStateCode(request.getPlaceOfSupplyStateCode())
                .totalGross(totalGross.setScale(SCALE, ROUNDING_MODE))
                .totalDiscount(totalDiscount.setScale(SCALE, ROUNDING_MODE))
                .totalTaxableValue(totalTaxable.setScale(SCALE, ROUNDING_MODE))
                .totalCgst(totalCgst.setScale(SCALE, ROUNDING_MODE))
                .totalSgst(totalSgst.setScale(SCALE, ROUNDING_MODE))
                .totalIgst(totalIgst.setScale(SCALE, ROUNDING_MODE))
                .totalTax(totalTax)
                .grandTotal(grandTotal)
                .items(itemResults)
                .build();
    }
}
