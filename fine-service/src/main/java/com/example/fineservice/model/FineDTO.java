package com.example.fineservice.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FineDTO {
    private Long id;
    private Long loanId;
    private String clientName;
    private String toolName;
    private String fineType;
    private int amount;
    private String status;
    private LocalDate creationDate;
    private LocalDate paymentDate;
}