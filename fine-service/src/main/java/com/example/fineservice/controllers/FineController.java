package com.example.fineservice.controllers;

import com.example.fineservice.model.FineDTO;
import com.example.fineservice.model.FineRequestDTO;
import com.example.fineservice.services.FineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fines")
public class FineController {

    @Autowired
    private FineService fineService;

    @GetMapping
    public ResponseEntity<List<FineDTO>> getAllFines() {
        List<FineDTO> fines = fineService.getAllFines();
        return ResponseEntity.ok(fines);
    }

    @GetMapping("/my-fines")
    public ResponseEntity<List<FineDTO>> getMyFines(JwtAuthenticationToken principal) {
        String keycloakId = principal.getName();
        List<FineDTO> fines = fineService.getFinesForUser(keycloakId);
        return ResponseEntity.ok(fines);
    }

    @PostMapping
    public ResponseEntity<Void> createFine(@RequestBody FineRequestDTO fineRequest) {
        fineService.createFine(fineRequest);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<String> payFine(@PathVariable Long id) {
        fineService.payFine(id);
        return ResponseEntity.ok("Multa pagada exitosamente.");
    }

    @GetMapping("/check-pending/{clientId}")
    public ResponseEntity<Boolean> checkPendingFines(@PathVariable Long clientId) {
        boolean hasPending = fineService.hasPendingFines(clientId);
        return ResponseEntity.ok(hasPending);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<FineDTO> getFineByLoanId(@PathVariable Long loanId) {
        FineDTO fine = fineService.getFineByLoanId(loanId);
        if (fine != null) {
            return ResponseEntity.ok(fine);
        }
        return ResponseEntity.notFound().build();
    }
}