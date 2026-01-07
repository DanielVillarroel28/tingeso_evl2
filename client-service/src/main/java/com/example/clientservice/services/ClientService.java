package com.example.clientservice.services;

import com.example.clientservice.entities.ClientEntity;
import com.example.clientservice.repositories.ClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Map;

@Service
public class ClientService {
    @Autowired
    ClientRepository clientRepository;

    public ArrayList<ClientEntity> getClients(){
        return (ArrayList<ClientEntity>) clientRepository.findAll();
    }

    public ClientEntity saveEmployee(ClientEntity employee){
        return clientRepository.save(employee);
    }

    public ClientEntity getClientById(Long id){
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + id));
    }

    public ClientEntity getClientByRut(String rut){
        return clientRepository.findByRut(rut);
    }

    public ClientEntity updateClient(Long id, ClientEntity clientDetails) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + id));

        client.setName(clientDetails.getName());
        client.setRut(clientDetails.getRut());
        client.setPhone(clientDetails.getPhone());
        client.setEmail(clientDetails.getEmail());
        client.setStatus(clientDetails.getStatus());

        return clientRepository.save(client);
    }


    public void updateClientStatus(Long id, String newStatus) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + id));

        client.setStatus(newStatus);
        clientRepository.save(client);
        System.out.println("Estado del cliente " + id + " actualizado a: " + newStatus);
    }


    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + id);
        }
        clientRepository.deleteById(id);
    }

    @Transactional
    public ClientEntity findOrCreateClient(JwtAuthenticationToken principal) {
        String keycloakId = principal.getName();
        return clientRepository.findByKeycloakId(keycloakId).orElseGet(() -> {
            System.out.println("Cliente con Keycloak ID '" + keycloakId + "' no encontrado. Creando nuevo cliente...");

            Map<String, Object> claims = principal.getToken().getClaims();

            String name = (String) claims.get("name");
            String email = (String) claims.get("email");
            String rut = (String) claims.get("RUT");
            String phone = (String) claims.get("phone");

            ClientEntity newClient = new ClientEntity();
            newClient.setKeycloakId(keycloakId);
            newClient.setName(name);
            newClient.setEmail(email);
            newClient.setRut(rut);
            newClient.setPhone(phone);
            newClient.setStatus("Activo");

            return clientRepository.save(newClient);
        });
    }

    public ClientEntity getCurrentClient(JwtAuthenticationToken principal) {
        String keycloakId = principal.getName();
        return clientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró un perfil de cliente para el usuario actual."));
    }

    public ClientEntity updateCurrentClient(JwtAuthenticationToken principal, ClientEntity clientDetails) {
        ClientEntity clientToUpdate = getCurrentClient(principal);

        clientToUpdate.setName(clientDetails.getName());
        clientToUpdate.setRut(clientDetails.getRut());
        clientToUpdate.setPhone(clientDetails.getPhone());
        clientToUpdate.setEmail(clientDetails.getEmail());

        return clientRepository.save(clientToUpdate);
    }
}