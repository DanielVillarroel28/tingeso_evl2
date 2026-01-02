package com.example.loanservice.controllers;

import com.example.loanservice.entities.LoanEntity;
import com.example.loanservice.model.LoanDTO;
import com.example.loanservice.model.LoanWithFineInfoDTO;
import com.example.loanservice.model.ReturnRequestDTO;
import com.example.loanservice.services.LoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@CrossOrigin("*")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @GetMapping("/")
    public ResponseEntity<List<LoanWithFineInfoDTO>> listLoans() {
        List<LoanWithFineInfoDTO> loans = loanService.getLoansWithFineInfo();
        return ResponseEntity.ok(loans);
    }

    @PutMapping("/")
    public ResponseEntity<LoanEntity> updateLoan(@RequestBody LoanEntity loan) {
        LoanEntity loanUpdated = loanService.updateLoan(loan);
        return ResponseEntity.ok(loanUpdated);
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<LoanWithFineInfoDTO> processReturn(
            @PathVariable Long id,
            @RequestBody ReturnRequestDTO returnRequest) {
        LoanWithFineInfoDTO updatedLoan = loanService.processReturn(id, returnRequest);
        return ResponseEntity.ok(updatedLoan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoanById(@PathVariable Long id) throws Exception {
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/")
    public ResponseEntity<LoanEntity> createLoan(
            @Valid @RequestBody LoanDTO loanRequest,
            JwtAuthenticationToken principal) {
        LoanEntity newLoan = loanService.createLoan(loanRequest, principal);
        return new ResponseEntity<>(newLoan, HttpStatus.CREATED);
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<LoanWithFineInfoDTO>> getMyLoans(JwtAuthenticationToken principal) {
        // name() suele corresponder al 'sub' del JWT
        String keycloakId = principal.getName();
        List<LoanWithFineInfoDTO> loans = loanService.getLoansForUser(keycloakId);
        return ResponseEntity.ok(loans);
    }
}
