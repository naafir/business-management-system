package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.expense.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseService {

    ExpenseResponseDto createExpense(CreateExpenseRequest request, UUID userId, String username);

    ExpenseResponseDto getExpenseById(UUID id);

    PageResponse<ExpenseResponseDto> listExpenses(
            String search, UUID categoryId,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    );

    ExpenseSummaryDto getExpenseSummary();

    List<ExpenseCategoryDto> listCategories();

    void deleteExpense(UUID id, UUID userId, String username);
}
