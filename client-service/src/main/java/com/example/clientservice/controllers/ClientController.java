package com.example.clientservice.controllers;

import com.example.clientservice.entities.ClientEntity;
import com.example.clientservice.services.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    @Autowired
    ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientEntity>> listClients(jakarta.servlet.http.HttpServletRequest request) {
        System.out.println("PETICIÓN RECIBIDA");
        System.out.println("URL Real que llegó: " + request.getRequestURI());

        List<ClientEntity> clients = clientService.getClients();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientEntity> getClientById(@PathVariable Long id) {
        ClientEntity client = clientService.getClientById(id);
        return ResponseEntity.ok(client);
    }

    @PostMapping
    public ResponseEntity<ClientEntity> saveClient(@Valid @RequestBody ClientEntity client) {
        ClientEntity clientNew = clientService.saveEmployee(client);
        return ResponseEntity.ok(clientNew);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientEntity> updateClient(@PathVariable Long id, @Valid @RequestBody ClientEntity clientDetails) {
        ClientEntity clientUpdated = clientService.updateClient(id, clientDetails);
        return ResponseEntity.ok(clientUpdated);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateClientStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {

        clientService.updateClientStatus(id, newStatus);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientById(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ClientEntity> getCurrentClient(@AuthenticationPrincipal JwtAuthenticationToken principal) {
        ClientEntity client = clientService.getCurrentClient(principal);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/me")
    public ResponseEntity<ClientEntity> updateCurrentClient(
            @AuthenticationPrincipal JwtAuthenticationToken principal,
            @Valid @RequestBody ClientEntity clientDetails
    ) {
        ClientEntity updatedClient = clientService.updateCurrentClient(principal, clientDetails);
        return ResponseEntity.ok(updatedClient);
    }
}