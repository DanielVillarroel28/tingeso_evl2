package com.example.fineservice.model;


import lombok.Data;

@Data
public class FineRequestDTO {
    private Long loanId;
    private Long clientId;
    private String clientName;     // M2 nos envía esto
    private String clientKeycloakId; // M2 nos envía esto
    private String toolName;       // M2 nos envía esto
    private String type;           // "Atraso", "Daño...", etc.
    private int amount;            // Si es 0, M4 calcula. Si > 0, se usa este valor.
    private long overdueDays;      // Opcional: para calcular multa por atraso
}