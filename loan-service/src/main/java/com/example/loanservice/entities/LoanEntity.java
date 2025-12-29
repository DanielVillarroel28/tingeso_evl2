package com.example.loanservice.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    // --- Referencias a otros microservicios (solo IDs y Snapshots) ---
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    private String clientName;
    private String clientKeycloakId;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;
    private String toolName;
    // ----------------------------------------------------------------

    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status; // "Activo", "Devuelto"

    // IMPORTANTE: Se elimina la lista de multas.
    // private List<FineEntity> fines;  <-- ESTO YA NO VA AQUÍ
}