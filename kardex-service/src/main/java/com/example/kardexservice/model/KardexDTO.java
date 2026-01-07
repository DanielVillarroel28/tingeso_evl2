package com.example.kardexservice.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KardexDTO {
    private Long toolId;
    private String toolName;
    private String movementType;
    private LocalDateTime movementDate;
    private int quantityAffected;
    private String userResponsible;
}