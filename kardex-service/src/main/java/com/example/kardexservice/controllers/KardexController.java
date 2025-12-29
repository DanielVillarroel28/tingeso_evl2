package com.example.kardexservice.controllers;

import com.example.kardexservice.entities.KardexEntity;
import com.example.kardexservice.services.KardexService; // Tu servicio refactorizado
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kardex")
public class KardexController {

    @Autowired
    private KardexService kardexService;

    // Este endpoint será consumido internamente por el ToolService (M1)
    @PostMapping("/movement")
    public ResponseEntity<KardexEntity> registerMovement(@RequestBody KardexEntity movement) {
        // Asumiendo que tu service solo hace el save()
        KardexEntity newMovement = kardexService.save(movement);
        return ResponseEntity.ok(newMovement);
    }
}