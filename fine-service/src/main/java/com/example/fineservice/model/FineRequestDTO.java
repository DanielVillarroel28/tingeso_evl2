package com.example.fineservice.model;


import lombok.Data;

@Data
public class FineRequestDTO {
    private Long loanId;
    private Long clientId;
    private String clientName;     
    private String clientKeycloakId; 
    private String toolName;       
    private String type;           
    private int amount;            
    private long overdueDays;     
}