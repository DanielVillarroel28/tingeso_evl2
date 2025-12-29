package com.example.kardexservice.repositories;

import com.example.kardexservice.entities.KardexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<KardexEntity, Long> {

    // Antes era findByTool_NameIgnoreCase, ahora es directo sobre el campo de la entidad Kardex
    List<KardexEntity> findByToolNameIgnoreCase(String toolName);

    List<KardexEntity> findByMovementDateGreaterThanEqualAndMovementDateLessThan(LocalDateTime start, LocalDateTime end);

    List<KardexEntity> findByToolNameIgnoreCaseAndMovementDateGreaterThanEqualAndMovementDateLessThan(String toolName, LocalDateTime start, LocalDateTime end);
}