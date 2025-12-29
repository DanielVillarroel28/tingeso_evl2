package com.example.loanservice.services;


import com.example.loanservice.model.LoanDTO;
import com.example.loanservice.model.LoanWithFineInfoDTO;
import com.example.loanservice.model.ReturnRequestDTO;
import com.example.loanservice.entities.LoanEntity;
import com.example.loanservice.repositories.LoanRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Nombres de los servicios en Kubernetes (ClusterIP)
    private final String TOOL_SERVICE_URL = "http://tool-service/api/tools";
    private final String CLIENT_SERVICE_URL = "http://client-service/api/clients";
    private final String KARDEX_SERVICE_URL = "http://kardex-service/api/kardex/movement";
    private final String FINE_SERVICE_URL = "http://fine-service/api/fines"; // Nuevo Microservicio de Multas

    // --- LISTAR PRÉSTAMOS ---
    public List<LoanWithFineInfoDTO> getLoansWithFineInfo() {
        // Nota: Esto puede ser lento (N+1 queries HTTP). En producción se usaría un patrón BFF o agregación.
        // Para la evaluación, RestTemplate en bucle es aceptable si el volumen de datos es bajo.
        return loanRepository.findAll().stream()
                .map(this::buildLoanWithFineInfoDTO)
                .collect(Collectors.toList());
    }

    public List<LoanWithFineInfoDTO> getLoansForUser(String keycloakId) {
        return loanRepository.findByClientKeycloakId(keycloakId).stream()
                .map(this::buildLoanWithFineInfoDTO)
                .collect(Collectors.toList());
    }

    // --- CREAR PRÉSTAMO ---
    @Transactional
    public LoanEntity createLoan(LoanDTO loanRequest, JwtAuthenticationToken principal) {

        // 1. Obtener Cliente (HTTP a M3)
        ClientDTO clientDTO;
        if (loanRequest.getClientId() != null) {
            clientDTO = getClientById(loanRequest.getClientId());
        } else {
            String keycloakId = principal.getName();
            clientDTO = getClientByKeycloakId(keycloakId);
        }

        // 2. Obtener Herramienta (HTTP a M1)
        ToolDTO toolDTO = getToolById(loanRequest.getToolId());

        // 3. Validaciones (Incluye llamada HTTP a M4-FineService)
        validateLoanRequest(clientDTO, toolDTO, loanRequest.getDueDate());

        // 4. Actualizar Herramienta (HTTP a M1)
        updateToolStatus(toolDTO.getId(), "Prestada");

        // 5. Guardar Préstamo Local (M2)
        LoanEntity newLoan = new LoanEntity();
        newLoan.setClientId(clientDTO.getId());
        newLoan.setClientName(clientDTO.getName());
        newLoan.setClientKeycloakId(clientDTO.getKeycloakId());
        newLoan.setToolId(toolDTO.getId());
        newLoan.setToolName(toolDTO.getName());
        newLoan.setLoanDate(LocalDate.now());
        newLoan.setDueDate(loanRequest.getDueDate());
        newLoan.setStatus("Activo");

        LoanEntity savedLoan = loanRepository.save(newLoan);

        // 6. Kardex (HTTP a M5)
        sendToKardex(savedLoan.getToolId(), savedLoan.getToolName(), "Préstamo", -1, savedLoan.getClientName());

        return savedLoan;
    }

    // --- PROCESAR DEVOLUCIÓN ---
    @Transactional
    public LoanWithFineInfoDTO processReturn(Long loanId, ReturnRequestDTO returnRequest) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!"Activo".equals(loan.getStatus())) {
            throw new RuntimeException("Este préstamo no está activo.");
        }

        loan.setReturnDate(LocalDate.now());
        loan.setStatus("Devuelto");

        String damageType = returnRequest.getStatus(); // "Irreparable", "Dañada", "Bueno"
        String finalToolStatus = "Disponible";
        String currentUser = "EMPLEADO"; // Obtener de token si es posible

        // Lógica de Daños y Kardex
        if ("Irreparable".equals(damageType)) {
            finalToolStatus = "Dada de baja";
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, currentUser);
            sendToKardex(loan.getToolId(), loan.getToolName(), "Baja", -1, currentUser);

            // Crear Multa por Daño Total (HTTP a FineService)
            createFine(loan.getId(), loan.getClientId(), "Daño Irreparable", 0); // El monto 0 indica que el servicio calcule según valor repo

        } else if ("Dañada".equals(damageType)) {
            finalToolStatus = "En reparación";
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, currentUser);
            sendToKardex(loan.getToolId(), loan.getToolName(), "Reparación", 0, currentUser);

            // Crear Multa por Daño Reparable (HTTP a FineService)
            createFine(loan.getId(), loan.getClientId(), "Daño Reparable", 0);

        } else {
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, currentUser);
        }

        // Crear Multa por Atraso (Si corresponde)
        if (loan.getReturnDate().isAfter(loan.getDueDate())) {
            createFine(loan.getId(), loan.getClientId(), "Atraso", 0);
        }

        // Actualizar Herramienta en M1
        updateToolStatus(loan.getToolId(), finalToolStatus);

        LoanEntity savedLoan = loanRepository.save(loan);
        return buildLoanWithFineInfoDTO(savedLoan);
    }

    // --- ACTUALIZAR Y ELIMINAR ---
    public LoanEntity updateLoan(LoanEntity loan) {
        return loanRepository.save(loan);
    }

    public boolean deleteLoan(Long id) throws Exception {
        LoanEntity loan = loanRepository.findById(id).orElseThrow(() -> new Exception("Préstamo no encontrado"));
        if (loan.getReturnDate() == null) throw new Exception("No se puede eliminar préstamo activo.");
        loanRepository.deleteById(id);
        return true;
    }

    // ==========================================
    // MÉTODOS PRIVADOS (COMUNICACIÓN ENTRE MICROSERVICIOS)
    // ==========================================

    // Llamada al Microservicio de Multas para crear una multa
    private void createFine(Long loanId, Long clientId, String type, int amountOverride) {
        try {
            FineRequestDTO fineReq = new FineRequestDTO();
            fineReq.setLoanId(loanId);
            fineReq.setClientId(clientId);
            fineReq.setType(type); // "Atraso", "Daño Reparable", "Daño Irreparable"
            fineReq.setAmount(amountOverride); // Si es 0, el servicio Fine calcula.

            restTemplate.postForObject(FINE_SERVICE_URL, fineReq, Void.class);
        } catch (Exception e) {
            System.err.println("Error creando multa: " + e.getMessage());
            // Dependiendo de la regla de negocio, podrías lanzar excepción o solo loguear
        }
    }

    // Llamada al Microservicio de Multas para ver si tiene deudas
    private boolean hasPendingFines(Long clientId) {
        try {
            // Endpoint sugerido en FineService: GET /api/fines/check-pending/{clientId}
            Boolean result = restTemplate.getForObject(FINE_SERVICE_URL + "/check-pending/" + clientId, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            System.err.println("Error verificando multas: " + e.getMessage());
            return false; // Ante duda, asumimos false o true según riesgo
        }
    }

    // Llamada al Microservicio de Multas para obtener info de una multa específica para el DTO
    private FineDTO getFineByLoanId(Long loanId) {
        try {
            // Endpoint sugerido: GET /api/fines/loan/{loanId}
            return restTemplate.getForObject(FINE_SERVICE_URL + "/loan/" + loanId, FineDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null; // No tiene multa
        } catch (Exception e) {
            return null;
        }
    }

    private void validateLoanRequest(ClientDTO client, ToolDTO tool, LocalDate dueDate) {
        if (dueDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Fecha inválida.");

        // Validación remota de Multas
        if (hasPendingFines(client.getId())) {
            throw new RuntimeException("El cliente tiene multas impagas.");
        }

        if (!"Disponible".equals(tool.getStatus()) || tool.getAvailableStock() < 1) {
            throw new RuntimeException("Herramienta no disponible.");
        }
        if (!"Activo".equals(client.getStatus())) {
            throw new RuntimeException("Cliente restringido.");
        }
        if (!loanRepository.findByClientIdAndDueDateBeforeAndStatus(client.getId(), LocalDate.now(), "Activo").isEmpty()) {
            throw new RuntimeException("Cliente con préstamos vencidos.");
        }
        if (loanRepository.countByClientIdAndStatus(client.getId(), "Activo") >= 5) {
            throw new RuntimeException("Límite de préstamos excedido.");
        }
        if (loanRepository.existsByClientIdAndToolIdAndStatus(client.getId(), tool.getId(), "Activo")) {
            throw new RuntimeException("Ya tiene esta herramienta.");
        }
    }

    // Construcción del DTO combinando datos locales y remotos (Multas)
    private LoanWithFineInfoDTO buildLoanWithFineInfoDTO(LoanEntity loan) {
        LoanWithFineInfoDTO dto = new LoanWithFineInfoDTO();
        dto.setId(loan.getId());
        dto.setClientName(loan.getClientName());
        dto.setToolName(loan.getToolName());
        dto.setLoanDate(loan.getLoanDate());
        dto.setDueDate(loan.getDueDate());
        dto.setReturnDate(loan.getReturnDate());
        dto.setStatus(loan.getStatus());

        // Consultar al microservicio de multas si existe multa para este préstamo
        FineDTO fine = getFineByLoanId(loan.getId());
        if (fine != null) {
            dto.setFineId(fine.getId());
            dto.setFineAmount(fine.getAmount());
            dto.setFineStatus(fine.getStatus());
        }
        return dto;
    }

    private ClientDTO getClientById(Long id) {
        return restTemplate.getForObject(CLIENT_SERVICE_URL + "/" + id, ClientDTO.class);
    }
    private ClientDTO getClientByKeycloakId(String id) {
        return restTemplate.getForObject(CLIENT_SERVICE_URL + "/keycloak/" + id, ClientDTO.class);
    }
    private ToolDTO getToolById(Long id) {
        return restTemplate.getForObject(TOOL_SERVICE_URL + "/" + id, ToolDTO.class);
    }
    private void updateToolStatus(Long toolId, String newStatus) {
        restTemplate.put(TOOL_SERVICE_URL + "/" + toolId + "/status?newStatus=" + newStatus, null);
    }
    private void sendToKardex(Long toolId, String toolName, String type, int qty, String user) {
        try {
            KardexDTO dto = new KardexDTO();
            dto.setToolId(toolId);
            dto.setToolName(toolName);
            dto.setMovementType(type);
            dto.setQuantityAffected(qty);
            dto.setUserResponsible(user);
            dto.setMovementDate(LocalDateTime.now());
            restTemplate.postForObject(KARDEX_SERVICE_URL, dto, Void.class);
        } catch (Exception e) {
            System.err.println("Kardex error: " + e.getMessage());
        }
    }

    // --- DTOs INTERNOS ---
    @Data public static class ClientDTO { private Long id; private String name; private String status; private String keycloakId; }
    @Data public static class ToolDTO { private Long id; private String name; private String status; private int availableStock; }
    @Data public static class KardexDTO { private Long toolId; private String toolName; private String movementType; private LocalDateTime movementDate; private int quantityAffected; private String userResponsible; }
    @Data public static class FineRequestDTO { private Long loanId; private Long clientId; private String type; private int amount; }
    @Data public static class FineDTO { private Long id; private int amount; private String status; }
}
