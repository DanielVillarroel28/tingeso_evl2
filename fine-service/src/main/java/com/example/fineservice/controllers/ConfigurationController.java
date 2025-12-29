package com.example.fineservice.controllers;

import com.example.fineservice.model.FeeUpdateRequestDTO;
import com.example.fineservice.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@CrossOrigin("*")
// @PreAuthorize("hasRole('ADMINISTRADOR')") //
public class ConfigurationController {

    @Autowired
    private ConfigurationService configurationService;


    @PutMapping("/rental-fee")
    public ResponseEntity<?> setRentalFee(@RequestBody FeeUpdateRequestDTO dto) {
        configurationService.updateFee("daily_rental_fee", dto.getValue());
        return ResponseEntity.ok("Tarifa de arriendo actualizada.");
    }


    @PutMapping("/late-fee")
    public ResponseEntity<?> setLateFee(@RequestBody FeeUpdateRequestDTO dto) {
        configurationService.updateFee("daily_late_fee", dto.getValue());
        return ResponseEntity.ok("Tarifa de multa actualizada.");
    }


    @GetMapping("/rental-fee")
    public ResponseEntity<Integer> getRentalFee() {
        return ResponseEntity.ok(configurationService.getFee("daily_rental_fee"));
    }


    @GetMapping("/late-fee")
    public ResponseEntity<Integer> getLateFee() {
        return ResponseEntity.ok(configurationService.getFee("daily_late_fee"));
    }


    @PutMapping("/repair-fee")
    public ResponseEntity<?> setRepairFee(@RequestBody FeeUpdateRequestDTO dto) {
        configurationService.updateFee("repair_fee", dto.getValue());
        return ResponseEntity.ok("Cargo por reparación actualizado.");
    }


    @GetMapping("/repair-fee")
    public ResponseEntity<Integer> getRepairFee() {
        return ResponseEntity.ok(configurationService.getFee("repair_fee"));
    }
}