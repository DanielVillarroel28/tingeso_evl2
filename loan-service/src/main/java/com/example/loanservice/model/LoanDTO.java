package com.example.loanservice.model;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanDTO {
    private Long clientId;
    @NotNull
    private Long toolId;
    @NotNull
    private LocalDate dueDate;
}