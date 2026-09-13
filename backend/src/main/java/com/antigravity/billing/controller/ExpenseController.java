package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.expense.*;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponseDto>>> listExpenses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ExpenseResponseDto> response = expenseService.listExpenses(search, categoryId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryDto>> getExpenseSummary() {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpenseSummary()));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryDto>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.listCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponseDto>> getExpenseById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpenseById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponseDto>> createExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        ExpenseResponseDto created = expenseService.createExpense(request, userDetails.getId(), userDetails.getUsername());
        return new ResponseEntity<>(ApiResponse.ok("Expense recorded successfully", created), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        expenseService.deleteExpense(id, userDetails.getId(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Expense deleted successfully", null));
    }
}
