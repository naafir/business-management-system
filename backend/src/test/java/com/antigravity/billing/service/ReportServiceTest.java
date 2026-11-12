package com.antigravity.billing.service;

import com.antigravity.billing.dto.report.*;
import com.antigravity.billing.repository.ExpenseRepository;
import com.antigravity.billing.repository.PurchaseRepository;
import com.antigravity.billing.repository.SaleRepository;
import com.antigravity.billing.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    @DisplayName("getGstOutputSummary: empty period returns zeroed financial metrics in a single query")
    void testGstOutputSummary_EmptyPeriod() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        // When no rows exist, aggregate query returns [0, 0, 0, 0, 0, 0, 0]
        Object[] emptyRow = new Object[]{
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        };
        when(saleRepository.getSaleGstAggregate(start, end))
                .thenReturn(Collections.singletonList(emptyRow));

        GstOutputSummaryDto summary = reportService.getGstOutputSummary(start, end);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalTaxableValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalCgstCollected()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalSgstCollected()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalIgstCollected()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalTaxCollected()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getB2bTaxableValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getB2cTaxableValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalInvoicesCount()).isEqualTo(0L);

        // Verify exactly 1 single consolidated query was executed (N+1 and repetitive calls eliminated)
        verify(saleRepository, times(1)).getSaleGstAggregate(start, end);
        verify(saleRepository, never()).sumRevenueByDateRange(any(), any());
        verify(saleRepository, never()).sumTaxableValueByDateRange(any(), any());
    }

    @Test
    @DisplayName("getGstOutputSummary: calculates accurate GST breakdown for multiple B2B and B2C transactions")
    void testGstOutputSummary_MultipleTransactions() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);

        // [grandTotal, taxableAmount, cgst, sgst, igst, b2bTaxable, count]
        Object[] aggRow = new Object[]{
                new BigDecimal("118000.00"), // grandTotal
                new BigDecimal("100000.00"), // taxableAmount
                new BigDecimal("6000.00"),   // cgst
                new BigDecimal("6000.00"),   // sgst
                new BigDecimal("6000.00"),   // igst
                new BigDecimal("70000.00"),  // b2bTaxable
                15L                          // count
        };
        when(saleRepository.getSaleGstAggregate(start, end))
                .thenReturn(Collections.singletonList(aggRow));

        GstOutputSummaryDto summary = reportService.getGstOutputSummary(start, end);

        assertThat(summary.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("118000.00"));
        assertThat(summary.getTotalTaxableValue()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(summary.getTotalCgstCollected()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(summary.getTotalSgstCollected()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(summary.getTotalIgstCollected()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(summary.getTotalTaxCollected()).isEqualByComparingTo(new BigDecimal("18000.00"));
        assertThat(summary.getB2bTaxableValue()).isEqualByComparingTo(new BigDecimal("70000.00"));
        assertThat(summary.getB2cTaxableValue()).isEqualByComparingTo(new BigDecimal("30000.00")); // 100,000 - 70,000
        assertThat(summary.getTotalInvoicesCount()).isEqualTo(15L);

        verify(saleRepository, times(1)).getSaleGstAggregate(start, end);
    }

    @Test
    @DisplayName("getGstInputSummary: calculates ITC accurately in a single consolidated query")
    void testGstInputSummary_CalculatesItc() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);

        // [grandTotal, taxableAmount, cgst, sgst, igst, count]
        Object[] purchRow = new Object[]{
                new BigDecimal("59000.00"), // grandTotal
                new BigDecimal("50000.00"), // taxableAmount
                new BigDecimal("2500.00"),  // cgst
                new BigDecimal("2500.00"),  // sgst
                new BigDecimal("4000.00"),  // igst
                8L                          // count
        };
        when(purchaseRepository.getPurchaseGstAggregate(start, end))
                .thenReturn(Collections.singletonList(purchRow));

        GstInputSummaryDto summary = reportService.getGstInputSummary(start, end);

        assertThat(summary.getTotalPurchaseValue()).isEqualByComparingTo(new BigDecimal("59000.00"));
        assertThat(summary.getTotalTaxableValue()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(summary.getTotalCgstPaid()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(summary.getTotalSgstPaid()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(summary.getTotalIgstPaid()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(summary.getTotalInputTaxCredit()).isEqualByComparingTo(new BigDecimal("9000.00")); // 2500 + 2500 + 4000
        assertThat(summary.getTotalPurchasesCount()).isEqualTo(8L);

        verify(purchaseRepository, times(1)).getPurchaseGstAggregate(start, end);
        verify(purchaseRepository, never()).sumPurchasesByDateRange(any(), any());
    }

    @Test
    @DisplayName("getHsnSummary: correctly maps grouped HSN lines and formats N/A")
    void testHsnSummary_FormattingAndTotals() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);

        List<Object[]> hsnRows = List.of(
                new Object[]{"8471", new BigDecimal("10.00"), new BigDecimal("10000.00"),
                        new BigDecimal("900.00"), new BigDecimal("900.00"), BigDecimal.ZERO, new BigDecimal("11800.00")},
                new Object[]{null, new BigDecimal("2.00"), new BigDecimal("2000.00"),
                        new BigDecimal("180.00"), new BigDecimal("180.00"), BigDecimal.ZERO, new BigDecimal("2360.00")}
        );
        when(saleRepository.hsnSummaryByDateRange(start, end)).thenReturn(hsnRows);

        List<HsnSummaryLineDto> lines = reportService.getHsnSummary(start, end);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getHsnSac()).isEqualTo("8471");
        assertThat(lines.get(0).getTotalTax()).isEqualByComparingTo(new BigDecimal("1800.00"));

        assertThat(lines.get(1).getHsnSac()).isEqualTo("N/A");
        assertThat(lines.get(1).getTotalTax()).isEqualByComparingTo(new BigDecimal("360.00"));
    }

    @Test
    @DisplayName("getMonthlyTrend: eliminates 12-loop query and consolidates expenses in single annual query")
    void testMonthlyTrend_EliminatesLoopQueries() {
        int year = 2026;
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        // Sales for Jan (month 1) and Mar (month 3)
        List<Object[]> salesTrend = List.of(
                new Object[]{2026, 1, new BigDecimal("50000.00"), 5L},
                new Object[]{2026, 3, new BigDecimal("75000.00"), 8L}
        );
        when(saleRepository.monthlySalesTrend(start, end)).thenReturn(salesTrend);

        // Purchases for Jan (month 1)
        List<Object[]> purchTrend = Collections.singletonList(
                new Object[]{2026, 1, new BigDecimal("30000.00"), 3L}
        );
        when(purchaseRepository.monthlyPurchasesTrend(start, end)).thenReturn(purchTrend);

        // Expenses for Jan (month 1) and Feb (month 2)
        List<Object[]> expTrend = List.of(
                new Object[]{2026, 1, new BigDecimal("5000.00"), 2L},
                new Object[]{2026, 2, new BigDecimal("7000.00"), 4L}
        );
        when(expenseRepository.monthlyExpensesTrend(start, end)).thenReturn(expTrend);

        List<MonthlyTrendDto> result = reportService.getMonthlyTrend(year);

        assertThat(result).hasSize(12);

        // Jan (Month 1)
        MonthlyTrendDto jan = result.get(0);
        assertThat(jan.getMonth()).isEqualTo(1);
        assertThat(jan.getSalesAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(jan.getPurchasesAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(jan.getExpensesAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(jan.getSalesCount()).isEqualTo(5L);
        assertThat(jan.getPurchasesCount()).isEqualTo(3L);

        // Feb (Month 2) - sales 0, exp 7000
        MonthlyTrendDto feb = result.get(1);
        assertThat(feb.getMonth()).isEqualTo(2);
        assertThat(feb.getSalesAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(feb.getExpensesAmount()).isEqualByComparingTo(new BigDecimal("7000.00"));

        // Crucial verification: expenseRepository.sumExpensesByDateRange must NOT be called in a loop (0 times)
        verify(expenseRepository, never()).sumExpensesByDateRange(any(), any());
        // Verify exactly 1 call each for the whole year
        verify(saleRepository, times(1)).monthlySalesTrend(start, end);
        verify(purchaseRepository, times(1)).monthlyPurchasesTrend(start, end);
        verify(expenseRepository, times(1)).monthlyExpensesTrend(start, end);
    }

    @Test
    @DisplayName("getProfitLoss: reduces 11 queries to 3 and computes 100% invariant financial results")
    void testProfitLoss_FinancialInvarianceAndQueryReduction() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        // Sales aggregate row: [grandTotal, taxableAmount, cgst, sgst, igst, b2bTaxable, count]
        Object[] saleRow = new Object[]{
                new BigDecimal("236000.00"), // grandTotal (revenue)
                new BigDecimal("200000.00"), // taxableAmount (revenueTaxable)
                new BigDecimal("12000.00"),  // cgst
                new BigDecimal("12000.00"),  // sgst
                new BigDecimal("12000.00"),  // igst
                new BigDecimal("150000.00"), // b2bTaxable
                20L
        };
        when(saleRepository.getSaleGstAggregate(start, end))
                .thenReturn(Collections.singletonList(saleRow));

        // Purchase aggregate row: [grandTotal, taxableAmount, cgst, sgst, igst, count]
        Object[] purchRow = new Object[]{
                new BigDecimal("118000.00"), // grandTotal (totalCogs)
                new BigDecimal("100000.00"), // taxableAmount (cogsTaxable)
                new BigDecimal("6000.00"),   // cgst
                new BigDecimal("6000.00"),   // sgst
                new BigDecimal("6000.00"),   // igst
                10L
        };
        when(purchaseRepository.getPurchaseGstAggregate(start, end))
                .thenReturn(Collections.singletonList(purchRow));

        // Expenses
        BigDecimal totalExpenses = new BigDecimal("25000.00");
        when(expenseRepository.sumExpensesByDateRange(start, end)).thenReturn(totalExpenses);

        ProfitLossDto pl = reportService.getProfitLoss(start, end);

        // Expected financial values:
        // revenue = 236000.00
        // cogs = 118000.00
        // grossProfit = revenueTaxable (200000) - cogsTaxable (100000) = 100000.00
        // netProfit = grossProfit (100000) - expenses (25000) = 75000.00
        // margin = (100000 * 100) / 200000 = 50.00%
        // taxCollected = 12000 + 12000 + 12000 = 36000.00
        // itc = 6000 + 6000 + 6000 = 18000.00
        // netTaxLiability = 36000 - 18000 = 18000.00
        assertThat(pl.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("236000.00"));
        assertThat(pl.getTotalCogs()).isEqualByComparingTo(new BigDecimal("118000.00"));
        assertThat(pl.getGrossProfit()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(pl.getGrossMarginPercent()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(pl.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(pl.getNetProfit()).isEqualByComparingTo(new BigDecimal("75000.00"));
        assertThat(pl.getTotalTaxCollected()).isEqualByComparingTo(new BigDecimal("36000.00"));
        assertThat(pl.getTotalInputTaxCredit()).isEqualByComparingTo(new BigDecimal("18000.00"));
        assertThat(pl.getNetTaxLiability()).isEqualByComparingTo(new BigDecimal("18000.00"));

        // Verify exactly 3 queries executed instead of 11 queries
        verify(saleRepository, times(1)).getSaleGstAggregate(start, end);
        verify(purchaseRepository, times(1)).getPurchaseGstAggregate(start, end);
        verify(expenseRepository, times(1)).sumExpensesByDateRange(start, end);

        // Verify old repetitive queries were never called
        verify(saleRepository, never()).sumRevenueByDateRange(any(), any());
        verify(purchaseRepository, never()).sumPurchasesByDateRange(any(), any());
        verify(saleRepository, never()).sumCgstByDateRange(any(), any());
    }
}
