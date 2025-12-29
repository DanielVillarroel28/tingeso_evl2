package com.example.fineservice.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "configurations")
@Data
public class ConfigurationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String configKey;
    private String configValue;
}
