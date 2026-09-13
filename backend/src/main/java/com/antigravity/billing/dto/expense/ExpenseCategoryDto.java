package com.antigravity.billing.dto.expense;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ExpenseCategoryDto {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
}
