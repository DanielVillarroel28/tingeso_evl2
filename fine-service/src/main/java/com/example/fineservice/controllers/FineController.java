package com.example.fineservice.controllers;

import com.example.fineservice.model.FineDTO;
import com.example.fineservice.services.FineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fines")
@CrossOrigin("*")
public class FineController {

    @Autowired
    private FineService fineService;


    @GetMapping("/")
    public ResponseEntity<List<FineDTO>> getAllFines() {
        List<FineDTO> fines = fineService.getAllFines();
        return ResponseEntity.ok(fines);
    }


    @GetMapping("/my-fines")
    public ResponseEntity<List<FineDTO>> getMyFines(JwtAuthenticationToken principal) {
        String keycloakId = principal.getName(); // Obtiene el 'sub' del token del usuario actual
        List<FineDTO> fines = fineService.getFinesForUser(keycloakId);
        return ResponseEntity.ok(fines);
    }


    @PutMapping("/{id}/pay")
    public ResponseEntity<String> payFine(@PathVariable Long id) {
        fineService.payFine(id);
        return ResponseEntity.ok("Multa pagada exitosamente.");
    }
}
