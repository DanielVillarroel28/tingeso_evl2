package com.example.fineservice.services;

import com.example.fineservice.model.FineDTO;
import com.example.fineservice.model.FineRequestDTO;
import com.example.fineservice.entities.FineEntity;
import com.example.fineservice.repositories.FineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FineService {

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RestTemplate restTemplate;
    @Value("${services.client.base-url:http://client-service:8080}")
    private String clientServiceBaseUrl;


    public List<FineDTO> getAllFines() {
        return fineRepository.findAll().stream()
                .map(this::buildFineDTO)
                .collect(Collectors.toList());
    }

    public List<FineDTO> getFinesForUser(String keycloakId) {
        return fineRepository.findByClientKeycloakId(keycloakId).stream()
                .map(this::buildFineDTO)
                .collect(Collectors.toList());
    }

    //Creacion de multas

    @Transactional
    public void createFine(FineRequestDTO request) {
        FineEntity fine = new FineEntity();
        fine.setLoanId(request.getLoanId());
        fine.setClientId(request.getClientId());
        fine.setClientName(request.getClientName());
        fine.setClientKeycloakId(request.getClientKeycloakId());
        fine.setToolName(request.getToolName());
        fine.setFineType(request.getType());
        fine.setCreationDate(LocalDate.now());
        fine.setStatus("Pendiente");

        String type = request.getType() != null ? request.getType().toLowerCase() : "";
        int calculatedAmount = 0;

        if (request.getAmount() > 0) {
            calculatedAmount = request.getAmount();
        } else {

            if (type.contains("atraso")) {
                int dailyFee = configurationService.getFee("daily_late_fee");
                long days = request.getOverdueDays() > 0 ? request.getOverdueDays() : 1;
                calculatedAmount = (int) (days * dailyFee);

            } else if (type.contains("reparable") || type.contains("dañada")) {
                calculatedAmount = configurationService.getFee("repair_fee");

            } else if (type.contains("irreparable")) {
                calculatedAmount = 50000; // Valor por defecto de seguridad
            }
        }

        fine.setAmount(calculatedAmount);

        // Guardamos solo si hay monto y restringimos al cliente
        if (fine.getAmount() > 0) {
            fineRepository.save(fine);
            System.out.println("Multa creada. Restringiendo cliente " + request.getClientId());
            updateClientStatus(request.getClientId(), "Restringido");
        }
    }

    //pago de multas

    @Transactional
    public void payFine(Long fineId) {
        FineEntity fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new RuntimeException("Multa no encontrada"));

        fine.setStatus("Pagada");
        fine.setPaymentDate(LocalDate.now());
        fineRepository.save(fine);

        List<FineEntity> pendingFines = fineRepository.findPendingFinesByClientId(fine.getClientId());

        if (pendingFines.isEmpty()) {
            System.out.println("Cliente " + fine.getClientId() + " sin deudas pendientes. Activando...");
            updateClientStatus(fine.getClientId(), "Activo");
        } else {
            System.out.println("Cliente " + fine.getClientId() + " aún tiene " + pendingFines.size() + " multas pendientes.");
        }
    }

    // Llamada a ClientService para actualizar estado del cliente

    private void updateClientStatus(Long clientId, String newStatus) {
        try {

            String url = clientServiceBaseUrl + "/api/v1/clients/" + clientId + "/status?newStatus=" + newStatus;

            System.out.println("Llamando a ClientService: " + url);
            HttpHeaders headers = new HttpHeaders();
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String token = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null) {
                    headers.set(HttpHeaders.AUTHORIZATION, token);
                }
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            System.out.println("Estado actualizado exitosamente a: " + newStatus);

        } catch (Exception e) {
            System.err.println("Error crítico comunicando con ClientService: " + e.getMessage());
        }
    }


    public boolean hasPendingFines(Long clientId) {
        return !fineRepository.findPendingFinesByClientId(clientId).isEmpty();
    }

    public FineDTO getFineByLoanId(Long loanId) {
        return fineRepository.findByLoanId(loanId)
                .map(this::buildFineDTO)
                .orElse(null);
    }


    private FineDTO buildFineDTO(FineEntity fine) {
        FineDTO dto = new FineDTO();
        dto.setId(fine.getId());
        dto.setLoanId(fine.getLoanId());
        dto.setClientName(fine.getClientName());
        dto.setToolName(fine.getToolName());
        dto.setFineType(fine.getFineType());
        dto.setAmount(fine.getAmount());
        dto.setStatus(fine.getStatus());
        dto.setCreationDate(fine.getCreationDate());
        dto.setPaymentDate(fine.getPaymentDate());
        return dto;
    }
}