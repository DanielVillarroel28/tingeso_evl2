package com.example.fineservice.services;


import com.example.fineservice.model.FineDTO;
import com.example.fineservice.model.FineRequestDTO;
import com.example.fineservice.entities.FineEntity;
import com.example.fineservice.repositories.FineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FineService {

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ConfigurationService configurationService; // Asumo que esto es local en M4

    @Autowired
    private RestTemplate restTemplate;

    // URL del servicio de Clientes (M3)
    private final String CLIENT_SERVICE_URL = "http://client-service/api/clients";

    // --- LISTADOS ---

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

    // --- CREACIÓN DE MULTAS (LLAMADO POR M2-LOANS) ---

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

        // LÓGICA DE CÁLCULO DE MONTO (Épica 4)
        if (request.getAmount() > 0) {
            // Caso: Daño irreparable (M2 envía el valor de reposición)
            fine.setAmount(request.getAmount());
        } else {
            // Caso: M4 debe calcular según tarifas configuradas
            int calculatedAmount = 0;
            if ("Atraso".equals(request.getType())) {
                int dailyFee = configurationService.getFee("daily_late_fee");
                calculatedAmount = (int) (request.getOverdueDays() * dailyFee);
            } else if ("Daño reparable".equals(request.getType())) {
                calculatedAmount = configurationService.getFee("repair_fee");
            }
            fine.setAmount(calculatedAmount);
        }

        // Solo guardamos si el monto es mayor a 0 (para evitar multas de $0 por atrasos de 0 días)
        if (fine.getAmount() > 0) {
            fineRepository.save(fine);
            // Bloquear cliente en M3
            updateClientStatus(request.getClientId(), "Restringido");
        }
    }

    // --- PAGO DE MULTAS ---

    @Transactional
    public void payFine(Long fineId) {
        FineEntity fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new RuntimeException("Multa no encontrada"));

        fine.setStatus("Pagada");
        fine.setPaymentDate(LocalDate.now());
        fineRepository.save(fine);

        // Validar si el cliente queda libre de deudas
        boolean hasPending = !fineRepository.findPendingFinesByClientId(fine.getClientId()).isEmpty();

        if (!hasPending) {
            // Desbloquear cliente en M3 (Comunicación HTTP)
            updateClientStatus(fine.getClientId(), "Activo");
        }
    }

    // --- CONSULTAS EXTERNAS (PARA M2-LOANS) ---

    public boolean hasPendingFines(Long clientId) {
        return !fineRepository.findPendingFinesByClientId(clientId).isEmpty();
    }

    public FineDTO getFineByLoanId(Long loanId) {
        return fineRepository.findByLoanId(loanId)
                .map(this::buildFineDTO)
                .orElse(null); // O lanzar 404
    }

    // --- HELPERS ---

    private void updateClientStatus(Long clientId, String newStatus) {
        try {
            // M3 debe tener endpoint PUT /api/clients/{id}/status?newStatus=...
            restTemplate.put(CLIENT_SERVICE_URL + "/" + clientId + "/status?newStatus=" + newStatus, null);
        } catch (Exception e) {
            System.err.println("Error actualizando estado cliente en M3: " + e.getMessage());
            // En sistemas reales usarías Kafka/RabbitMQ para garantizar esto
        }
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