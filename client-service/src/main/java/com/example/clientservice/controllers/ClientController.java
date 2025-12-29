package com.example.clientservice.controllers;

import com.example.clientservice.entities.ClientEntity;
import com.example.clientservice.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@CrossOrigin("*")
public class ClientController {

    @Autowired
    ClientService clientService;


    @GetMapping("/")
    public ResponseEntity<List<ClientEntity>> listClients() {
        List<ClientEntity> clients = clientService.getClients();
        return ResponseEntity.ok(clients);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ClientEntity> getClientById(@PathVariable Long id) {
        ClientEntity client = clientService.getClientById(id);
        return ResponseEntity.ok(client);
    }


    @PostMapping("/")
    public ResponseEntity<ClientEntity> saveClient(@RequestBody ClientEntity employee) {
        ClientEntity clientNew = clientService.saveEmployee(employee);
        return ResponseEntity.ok(clientNew);
    }


    @PutMapping("/{id}")
    public ResponseEntity<ClientEntity> updateClient(@PathVariable Long id, @RequestBody ClientEntity clientDetails){
        ClientEntity clientUpdated = clientService.updateClient(id, clientDetails);
        return ResponseEntity.ok(clientUpdated);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteClientById(@PathVariable Long id) throws Exception {
        var isDeleted = clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ClientEntity> getCurrentClient(@AuthenticationPrincipal JwtAuthenticationToken principal) {
        ClientEntity client = clientService.getCurrentClient(principal);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/me")
    public ResponseEntity<ClientEntity> updateCurrentClient(@AuthenticationPrincipal JwtAuthenticationToken principal, @RequestBody ClientEntity clientDetails) {
        ClientEntity updatedClient = clientService.updateCurrentClient(principal, clientDetails);
        return ResponseEntity.ok(updatedClient);
    }

}
