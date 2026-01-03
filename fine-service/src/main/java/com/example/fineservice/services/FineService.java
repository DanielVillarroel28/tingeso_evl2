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
    private ConfigurationService configurationService;

    @Autowired
    private RestTemplate restTemplate;

    // --- URL DEL SERVICIO DE CLIENTES (VÍA GATEWAY) ---
    // Usamos localhost:8080 para que el Gateway maneje el enrutamiento y los prefijos.
    private final String CLIENT_SERVICE_URL = "http://localhost:8080/CLIENT-SERVICE/api/v1/clients";

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

        // Guardamos nombres para historial
        fine.setClientName(request.getClientName());
        fine.setClientKeycloakId(request.getClientKeycloakId());
        fine.setToolName(request.getToolName());

        fine.setFineType(request.getType());
        fine.setCreationDate(LocalDate.now());
        fine.setStatus("Pendiente");

        // Normalizamos el tipo a minúsculas para comparar sin errores
        String type = request.getType() != null ? request.getType().toLowerCase() : "";

        // LÓGICA DE CÁLCULO DE MONTO
        if (request.getAmount() > 0) {
            // Caso 1: M2 ya envió un monto explícito (ej: valor de reposición por daño total)
            fine.setAmount(request.getAmount());
        } else {
            // Caso 2: El monto viene en 0, debemos calcularlo según tarifas configuradas
            int calculatedAmount = 0;

            if (type.contains("atraso")) {
                int dailyFee = configurationService.getFee("daily_late_fee");
                // Usamos los días de atraso que vienen del DTO, mínimo 1
                long days = request.getOverdueDays() > 0 ? request.getOverdueDays() : 1;
                calculatedAmount = (int) (days * dailyFee);

            } else if (type.contains("reparable") || type.contains("dañada")) {
                // Tarifa fija por reparación
                calculatedAmount = configurationService.getFee("repair_fee");

            } else if (type.contains("irreparable")) {
                // Fallback por si acaso llega un daño irreparable con monto 0
                // (aunque LoanService ya debería haber enviado el precio real)
                calculatedAmount = 50000;
            }

            fine.setAmount(calculatedAmount);
        }

        // GUARDAR SOLO SI EL MONTO ES > 0
        if (fine.getAmount() > 0) {
            fineRepository.save(fine);

            // --- AQUÍ LLAMAMOS A CLIENT-SERVICE PARA BLOQUEARLO ---
            updateClientStatus(request.getClientId(), "Restringido");

        } else {
            System.err.println("ADVERTENCIA: Se intentó crear multa tipo '" + request.getType() + "' pero el monto calculado fue 0. NO SE GUARDÓ.");
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

        // Verificar si el cliente ya no tiene deudas pendientes
        boolean hasPending = !fineRepository.findPendingFinesByClientId(fine.getClientId()).isEmpty();

        if (!hasPending) {
            // --- SI PAGÓ TODO, LO DESBLOQUEAMOS EN CLIENT-SERVICE ---
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
                .orElse(null);
    }

    // --- MÉTODO PRIVADO PARA LLAMAR A CLIENT-SERVICE ---

    private void updateClientStatus(Long clientId, String newStatus) {
        try {
            // Construimos la URL: http://localhost:8080/CLIENT-SERVICE/api/v1/clients/{id}/status?newStatus=...
            String url = CLIENT_SERVICE_URL + "/" + clientId + "/status?newStatus=" + newStatus;

            // Hacemos la petición PUT
            restTemplate.put(url, null);

            System.out.println("Solicitud enviada a ClientService: Cambiar cliente " + clientId + " a estado " + newStatus);

        } catch (Exception e) {
            System.err.println("Error crítico comunicando con ClientService: " + e.getMessage());
            // Nota: En un entorno productivo, aquí deberías usar un patrón Circuit Breaker
            // o cola de mensajes para reintentar luego.
        }
    }

    // --- MAPPER ---
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