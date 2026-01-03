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
// IMPORTANTE: No usar @CrossOrigin aquí porque el Gateway ya lo maneja
public class FineController {

    @Autowired
    private FineService fineService;

    // 1. Listar todas las multas (Admin)
    @GetMapping
    public ResponseEntity<List<FineDTO>> getAllFines() {
        List<FineDTO> fines = fineService.getAllFines();
        return ResponseEntity.ok(fines);
    }

    // 2. Listar mis multas (Usuario logueado)
    @GetMapping("/my-fines")
    public ResponseEntity<List<FineDTO>> getMyFines(JwtAuthenticationToken principal) {
        String keycloakId = principal.getName();
        List<FineDTO> fines = fineService.getFinesForUser(keycloakId);
        return ResponseEntity.ok(fines);
    }

    // 3. Crear una multa (Llamado internamente por LoanService)
    @PostMapping
    public ResponseEntity<Void> createFine(@RequestBody FineRequestDTO fineRequest) {
        fineService.createFine(fineRequest);
        return ResponseEntity.ok().build();
    }

    // 4. Pagar una multa
    @PutMapping("/{id}/pay")
    public ResponseEntity<String> payFine(@PathVariable Long id) {
        fineService.payFine(id);
        return ResponseEntity.ok("Multa pagada exitosamente.");
    }

    // 5. Verificar si un cliente tiene deuda (Llamado por LoanService antes de prestar)
    @GetMapping("/check-pending/{clientId}")
    public ResponseEntity<Boolean> checkPendingFines(@PathVariable Long clientId) {
        boolean hasPending = fineService.hasPendingFines(clientId);
        return ResponseEntity.ok(hasPending);
    }

    // 6. Obtener multa por ID de préstamo (Llamado por LoanService para armar el DTO)
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<FineDTO> getFineByLoanId(@PathVariable Long loanId) {
        FineDTO fine = fineService.getFineByLoanId(loanId);
        if (fine != null) {
            return ResponseEntity.ok(fine);
        }
        // Si no hay multa asociada, devolvemos 404 o null, según prefieras.
        // Aquí devolvemos 404 para que el RestTemplate lo maneje.
        return ResponseEntity.notFound().build();
    }
}