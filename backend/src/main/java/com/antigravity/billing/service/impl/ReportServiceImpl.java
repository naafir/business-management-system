package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.report.*;
import com.antigravity.billing.repository.ExpenseRepository;
import com.antigravity.billing.repository.PurchaseRepository;
import com.antigravity.billing.repository.SaleRepository;
import com.antigravity.billing.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    @Transactional(readOnly = true)
    public GstOutputSummaryDto getGstOutputSummary(LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = saleRepository.getSaleGstAggregate(startDate, endDate);
        Object[] row = (!rows.isEmpty() && rows.get(0) != null) ? rows.get(0) : new Object[7];

        BigDecimal revenue = toBd(row[0]);
        BigDecimal taxable = toBd(row[1]);
        BigDecimal cgst = toBd(row[2]);
        BigDecimal sgst = toBd(row[3]);
        BigDecimal igst = toBd(row[4]);
        BigDecimal b2bTaxable = toBd(row[5]);
        long count = toLong(row[6]);

        return GstOutputSummaryDto.builder()
                .totalRevenue(revenue)
                .totalTaxableValue(taxable)
                .totalCgstCollected(cgst)
                .totalSgstCollected(sgst)
                .totalIgstCollected(igst)
                .totalTaxCollected(cgst.add(sgst).add(igst))
                .b2bTaxableValue(b2bTaxable)
                .b2cTaxableValue(taxable.subtract(b2bTaxable))
                .totalInvoicesCount(count)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GstInputSummaryDto getGstInputSummary(LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = purchaseRepository.getPurchaseGstAggregate(startDate, endDate);
        Object[] row = (!rows.isEmpty() && rows.get(0) != null) ? rows.get(0) : new Object[6];

        BigDecimal purchases = toBd(row[0]);
        BigDecimal taxable = toBd(row[1]);
        BigDecimal cgst = toBd(row[2]);
        BigDecimal sgst = toBd(row[3]);
        BigDecimal igst = toBd(row[4]);
        long count = toLong(row[5]);

        return GstInputSummaryDto.builder()
                .totalPurchaseValue(purchases)
                .totalTaxableValue(taxable)
                .totalCgstPaid(cgst)
                .totalSgstPaid(sgst)
                .totalIgstPaid(igst)
                .totalInputTaxCredit(cgst.add(sgst).add(igst))
                .totalPurchasesCount(count)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HsnSummaryLineDto> getHsnSummary(LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = saleRepository.hsnSummaryByDateRange(startDate, endDate);
        List<HsnSummaryLineDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String hsnSac = (String) row[0];
            if (hsnSac == null || hsnSac.isBlank()) hsnSac = "N/A";
            BigDecimal qty = toBd(row[1]);
            BigDecimal taxable = toBd(row[2]);
            BigDecimal cgst = toBd(row[3]);
            BigDecimal sgst = toBd(row[4]);
            BigDecimal igst = toBd(row[5]);
            BigDecimal total = toBd(row[6]);
            result.add(HsnSummaryLineDto.builder()
                    .hsnSac(hsnSac)
                    .totalQuantity(qty)
                    .totalTaxableValue(taxable)
                    .totalCgst(cgst)
                    .totalSgst(sgst)
                    .totalIgst(igst)
                    .totalTax(cgst.add(sgst).add(igst))
                    .totalAmount(total)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyTrendDto> getMonthlyTrend(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        // Build map month -> sales
        Map<Integer, BigDecimal[]> salesMap = new HashMap<>();
        Map<Integer, Long> salesCountMap = new HashMap<>();
        for (Object[] row : saleRepository.monthlySalesTrend(start, end)) {
            int month = ((Number) row[1]).intValue();
            salesMap.put(month, new BigDecimal[]{toBd(row[2])});
            salesCountMap.put(month, ((Number) row[3]).longValue());
        }

        // Build map month -> purchases
        Map<Integer, BigDecimal[]> purchMap = new HashMap<>();
        Map<Integer, Long> purchCountMap = new HashMap<>();
        for (Object[] row : purchaseRepository.monthlyPurchasesTrend(start, end)) {
            int month = ((Number) row[1]).intValue();
            purchMap.put(month, new BigDecimal[]{toBd(row[2])});
            purchCountMap.put(month, ((Number) row[3]).longValue());
        }

        // Build map month -> expenses (single aggregate query for full year replacing 12 monthly loop queries)
        Map<Integer, BigDecimal> expensesMap = new HashMap<>();
        for (Object[] row : expenseRepository.monthlyExpensesTrend(start, end)) {
            int month = ((Number) row[1]).intValue();
            expensesMap.put(month, toBd(row[2]));
        }

        List<MonthlyTrendDto> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal salesAmt = salesMap.containsKey(m) ? salesMap.get(m)[0] : BigDecimal.ZERO;
            BigDecimal purchAmt = purchMap.containsKey(m) ? purchMap.get(m)[0] : BigDecimal.ZERO;
            BigDecimal expAmt = expensesMap.getOrDefault(m, BigDecimal.ZERO);
            long sc = salesCountMap.getOrDefault(m, 0L);
            long pc = purchCountMap.getOrDefault(m, 0L);

            result.add(MonthlyTrendDto.builder()
                    .year(year)
                    .month(m)
                    .monthName(Month.of(m).name().charAt(0) + Month.of(m).name().substring(1, 3).toLowerCase())
                    .salesAmount(salesAmt)
                    .purchasesAmount(purchAmt)
                    .expensesAmount(expAmt)
                    .salesCount(sc)
                    .purchasesCount(pc)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitLossDto getProfitLoss(LocalDate startDate, LocalDate endDate) {
        List<Object[]> saleRows = saleRepository.getSaleGstAggregate(startDate, endDate);
        Object[] saleRow = (!saleRows.isEmpty() && saleRows.get(0) != null) ? saleRows.get(0) : new Object[7];
        BigDecimal revenue = toBd(saleRow[0]);
        BigDecimal revenueTaxable = toBd(saleRow[1]);
        BigDecimal saleCgst = toBd(saleRow[2]);
        BigDecimal saleSgst = toBd(saleRow[3]);
        BigDecimal saleIgst = toBd(saleRow[4]);
        BigDecimal taxCollected = saleCgst.add(saleSgst).add(saleIgst);

        List<Object[]> purchRows = purchaseRepository.getPurchaseGstAggregate(startDate, endDate);
        Object[] purchRow = (!purchRows.isEmpty() && purchRows.get(0) != null) ? purchRows.get(0) : new Object[6];
        BigDecimal purchases = toBd(purchRow[0]);
        BigDecimal cogsTaxable = toBd(purchRow[1]);
        BigDecimal purchCgst = toBd(purchRow[2]);
        BigDecimal purchSgst = toBd(purchRow[3]);
        BigDecimal purchIgst = toBd(purchRow[4]);
        BigDecimal itc = purchCgst.add(purchSgst).add(purchIgst);

        BigDecimal expenses = expenseRepository.sumExpensesByDateRange(startDate, endDate);

        // Gross profit = Revenue (excl GST) - COGS (purchases excl GST)
        BigDecimal grossProfit = revenueTaxable.subtract(cogsTaxable);
        BigDecimal netProfit = grossProfit.subtract(expenses);
        BigDecimal margin = revenueTaxable.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.multiply(new BigDecimal("100")).divide(revenueTaxable, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ProfitLossDto.builder()
                .totalRevenue(revenue)
                .totalCogs(purchases)
                .grossProfit(grossProfit)
                .grossMarginPercent(margin)
                .totalExpenses(expenses)
                .netProfit(netProfit)
                .totalTaxCollected(taxCollected)
                .totalInputTaxCredit(itc)
                .netTaxLiability(taxCollected.subtract(itc))
                .build();
    }

    private BigDecimal toBd(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
