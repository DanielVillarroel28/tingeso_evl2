package com.example.clientservice.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @NotEmpty
    private String name;
    private String rut;
    private String phone;

    @NotEmpty
    private String email;
    private String stateClient;
    private String status;

    @Column(unique = true) // Cada usuario de Keycloak solo puede tener un cliente
    private String keycloakId;
}