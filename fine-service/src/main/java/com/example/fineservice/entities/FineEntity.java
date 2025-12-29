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

    // --- RELACIONES LÓGICAS (Solo IDs) ---
    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    // Guardamos estos datos para mostrarlos en el Frontend sin llamar a otros microservicios
    private String clientName;
    private String clientKeycloakId; // Para buscar "Mis Multas"
    private String toolName;
    // -------------------------------------

    private String fineType; // "Atraso", "Daño irreparable", "Daño reparable"
    private int amount;
    private String status; // "Pendiente", "Pagada"

    private LocalDate creationDate;
    private LocalDate paymentDate;
}