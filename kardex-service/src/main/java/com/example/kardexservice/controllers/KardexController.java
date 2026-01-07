package com.example.kardexservice.controllers;

import com.example.kardexservice.entities.KardexEntity;
import com.example.kardexservice.services.KardexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kardex") 
public class KardexController {

    @Autowired
    private KardexService kardexService;

    @PostMapping("/movement")
    public ResponseEntity<KardexEntity> registerMovement(@RequestBody KardexEntity movement) {
        KardexEntity newMovement = kardexService.save(movement);
        return ResponseEntity.ok(newMovement);
    }

    @GetMapping("/")
    public ResponseEntity<List<KardexEntity>> getAllMovements(
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {

        List<KardexEntity> movements = kardexService.getMovements(toolName, startDate, endDate);
        return ResponseEntity.ok(movements);
    }
}