package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.expense.*;
import com.antigravity.billing.entity.Expense;
import com.antigravity.billing.entity.ExpenseCategory;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.ExpenseCategoryRepository;
import com.antigravity.billing.repository.ExpenseRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ExpenseResponseDto createExpense(CreateExpenseRequest request, UUID userId, String username) {
        ExpenseCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", "id", request.getCategoryId()));
        }

        BigDecimal gstAmount = request.getGstAmount() != null ? request.getGstAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = request.getAmount().add(gstAmount);

        Expense expense = Expense.builder()
                .category(category)
                .expenseDate(request.getExpenseDate())
                .vendorName(request.getVendorName())
                .description(request.getDescription())
                .amount(request.getAmount())
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .gstEligible(request.isGstEligible())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .createdBy(userId)
                .createdByName(username)
                .build();

        Expense saved = expenseRepository.save(expense);

        auditService.logAction(userId, username, "CREATE_EXPENSE", "Expense",
                saved.getId().toString(),
                "Created expense: " + saved.getDescription() + " amount: " + saved.getTotalAmount());

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponseDto getExpenseById(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        return mapToDto(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponseDto> listExpenses(
            String search, UUID categoryId,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable
    ) {
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Expense> page = expenseRepository.findExpensesWithFilters(searchTerm, categoryId, startDate, endDate, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryDto getExpenseSummary() {
        BigDecimal total = expenseRepository.sumTotalExpenses();
        long count = expenseRepository.count();
        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end = LocalDate.now();
        BigDecimal eligibleGst = expenseRepository.sumEligibleGstByDateRange(start, end);

        return ExpenseSummaryDto.builder()
                .totalExpensesAmount(total)
                .eligibleGstInputAmount(eligibleGst)
                .totalExpensesCount(count)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategoryDto> listCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> ExpenseCategoryDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .active(c.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteExpense(UUID id, UUID userId, String username) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        expenseRepository.delete(expense);
        auditService.logAction(userId, username, "DELETE_EXPENSE", "Expense",
                id.toString(), "Deleted expense: " + expense.getDescription());
    }

    private ExpenseResponseDto mapToDto(Expense e) {
        return ExpenseResponseDto.builder()
                .id(e.getId())
                .categoryId(e.getCategory() != null ? e.getCategory().getId() : null)
                .categoryName(e.getCategory() != null ? e.getCategory().getName() : null)
                .expenseDate(e.getExpenseDate())
                .vendorName(e.getVendorName())
                .description(e.getDescription())
                .amount(e.getAmount())
                .gstAmount(e.getGstAmount())
                .totalAmount(e.getTotalAmount())
                .gstEligible(e.isGstEligible())
                .paymentMethod(e.getPaymentMethod())
                .referenceNumber(e.getReferenceNumber())
                .notes(e.getNotes())
                .createdByName(e.getCreatedByName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
