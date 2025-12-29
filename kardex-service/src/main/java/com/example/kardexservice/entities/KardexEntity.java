package com.example.kardexservice.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "kardex")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KardexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- CAMBIO IMPORTANTE ---
    // Eliminamos: private ToolEntity tool;
    // Agregamos el ID para referencia lógica
    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    // Opcional: Guardar el nombre para no tener que pedirlo al M1 al generar reportes
    private String toolName;
    // -------------------------

    private String movementType;
    private LocalDateTime movementDate;
    private int quantityAffected;
    private String userResponsible;
}