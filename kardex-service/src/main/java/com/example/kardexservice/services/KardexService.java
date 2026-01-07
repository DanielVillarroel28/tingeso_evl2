package com.example.kardexservice.services;


import com.example.kardexservice.entities.KardexEntity;
import com.example.kardexservice.repositories.KardexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KardexService {

    @Autowired
    private KardexRepository kardexRepository;


    //Registra un movimiento en el Kardex.

    public KardexEntity save(KardexEntity movement) {
        // Validación: Si no viene fecha, asignamos la actual
        if (movement.getMovementDate() == null) {
            movement.setMovementDate(LocalDateTime.now());
        }

        // Guardamos en la base de datos propia del microservicio M5
        return kardexRepository.save(movement);
    }

    /**
    Obtiene el historial de movimientos
    Soporta filtros por nombre de herramienta y rango de fechas.
     */
    public List<KardexEntity> getMovements(String toolName, LocalDate startDate, LocalDate endDate) {
        // Convertir LocalDate a LocalDateTime para cubrir todo el día
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        boolean hasToolName = toolName != null && !toolName.isEmpty();
        boolean hasDateRange = startDateTime != null && endDateTime != null;

        if (hasToolName && hasDateRange) {
            // Busca por Nombre Y Rango de fechas
            return kardexRepository.findByToolNameIgnoreCaseAndMovementDateGreaterThanEqualAndMovementDateLessThan(
                    toolName, startDateTime, endDateTime);
        } else if (hasToolName) {
            // Busca solo por Nombre
            return kardexRepository.findByToolNameIgnoreCase(toolName);
        } else if (hasDateRange) {
            // Busca solo por Rango de fechas
            return kardexRepository.findByMovementDateGreaterThanEqualAndMovementDateLessThan(
                    startDateTime, endDateTime);
        } else {
            return kardexRepository.findAll();
        }
    }
}