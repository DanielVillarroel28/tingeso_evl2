package com.example.loanservice.model;
import lombok.Data;
import java.time.LocalDate;

@Data // Lombok para generar getters y setters automáticamente
public class LoanWithFineInfoDTO {
    private Long id;
    private String clientName;
    private String toolName;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;

    // Campos adicionales para la multa
    private Long fineId;
    private Integer fineAmount;
    private String fineStatus;
}