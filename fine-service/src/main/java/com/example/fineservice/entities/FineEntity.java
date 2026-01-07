package com.example.fineservice.entities;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "fines")
@Data
public class FineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Relaciones logicas (Solo IDs)
    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    private String clientName;
    private String clientKeycloakId;
    private String toolName;

    private String fineType; // "Atraso", "Daño irreparable", "Daño reparable"
    private int amount;
    private String status; // "Pendiente", "Pagada"

    private LocalDate creationDate;
    private LocalDate paymentDate;
}