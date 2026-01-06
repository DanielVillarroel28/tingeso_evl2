package com.example.kardexservice.controllers;

import com.example.kardexservice.entities.KardexEntity;
import com.example.kardexservice.services.KardexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kardex") // Asegúrate que sea /v1
public class KardexController {

    @Autowired
    private KardexService kardexService;

    // 1. Registrar movimiento (Interno para Tool/Loan Service)
    @PostMapping("/movement")
    public ResponseEntity<KardexEntity> registerMovement(@RequestBody KardexEntity movement) {
        KardexEntity newMovement = kardexService.save(movement);
        return ResponseEntity.ok(newMovement);
    }

    // 2. Obtener historial (Este es el que le falta a tu Frontend)
    @GetMapping("/")
    public ResponseEntity<List<KardexEntity>> getAllMovements(
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        // Llama al servicio que ya tienes listo con los filtros
        List<KardexEntity> movements = kardexService.getMovements(toolName, startDate, endDate);
        return ResponseEntity.ok(movements);
    }
}