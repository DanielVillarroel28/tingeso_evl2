package com.example.loanservice.services;

import com.example.loanservice.entities.LoanEntity;
import com.example.loanservice.model.LoanDTO;
import com.example.loanservice.model.LoanWithFineInfoDTO;
import com.example.loanservice.model.ReturnRequestDTO;
import com.example.loanservice.repositories.LoanRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    @Value("${services.tool.base-url}")
    private String toolServiceBaseUrl;

    @Value("${services.client.base-url}")
    private String clientServiceBaseUrl;

    @Value("${services.kardex.base-url}")
    private String kardexServiceBaseUrl;

    @Value("${services.fine.base-url}")
    private String fineServiceBaseUrl;

    // --- LISTAR PRÉSTAMOS ---
    public List<LoanWithFineInfoDTO> getLoansWithFineInfo() {
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

        // 1) Obtener Cliente
        ClientDTO clientDTO;
        if (loanRequest.getClientId() != null) {
            clientDTO = getClientById(loanRequest.getClientId(), principal);
        } else {
            if (principal == null) {
                throw new IllegalArgumentException("Falta clientId o token JWT: envía clientId en el body o Authorization: Bearer <token>.");
            }
            clientDTO = getCurrentClient(principal);
        }

        // 2) Obtener Herramienta
        ToolDTO toolDTO = getToolById(loanRequest.getToolId(), principal);

        // 3) Validaciones
        validateLoanRequest(clientDTO, toolDTO, loanRequest.getDueDate(), principal);

        // 4) Actualizar Herramienta
        updateToolStatus(toolDTO.getId(), "Prestada", principal);

        // 5) Guardar Préstamo
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

        // 6) Kardex
        sendToKardex(savedLoan.getToolId(), savedLoan.getToolName(), "Préstamo", -1, savedLoan.getClientName(), principal);

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

        String damageType = returnRequest.getStatus();
        String finalToolStatus = "Disponible";
        String currentUser = "EMPLEADO";

        if ("Irreparable".equals(damageType)) {
            finalToolStatus = "Dada de baja";
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, currentUser, null);
            sendToKardex(loan.getToolId(), loan.getToolName(), "Baja", -1, currentUser, null);
            createFine(loan.getId(), loan.getClientId(), "Daño Irreparable", 0, null);

        } else if ("Dañada".equals(damageType)) {
            finalToolStatus = "En reparación";
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, currentUser, null);
            sendToKardex(loan.getToolId(), loan.getToolName(), "Reparación", 0, currentUser, null);
            createFine(loan.getId(), loan.getClientId(), "Daño Reparable", 0, null);

        } else {
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, currentUser, null);
        }

        if (loan.getReturnDate().isAfter(loan.getDueDate())) {
            createFine(loan.getId(), loan.getClientId(), "Atraso", 0, null);
        }

        updateToolStatus(loan.getToolId(), finalToolStatus, null);

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

    private void createFine(Long loanId, Long clientId, String type, int amountOverride, JwtAuthenticationToken principal) {
        try {
            FineRequestDTO fineReq = new FineRequestDTO();
            fineReq.setLoanId(loanId);
            fineReq.setClientId(clientId);
            fineReq.setType(type);
            fineReq.setAmount(amountOverride);

            HttpEntity<FineRequestDTO> entity = new HttpEntity<>(fineReq, authHeaders(principal));
            restTemplate.exchange(
                    fineServiceBaseUrl + "/api/v1/fines",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
        } catch (Exception e) {
            System.err.println("Error creando multa: " + e.getMessage());
        }
    }

    private boolean hasPendingFines(Long clientId, JwtAuthenticationToken principal) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    fineServiceBaseUrl + "/api/v1/fines/check-pending/" + clientId,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );
            Boolean result = response.getBody();
            return result != null && result;
        } catch (Exception e) {
            System.err.println("Error verificando multas: " + e.getMessage());
            return false;
        }
    }

    private FineDTO getFineByLoanId(Long loanId) {
        try {
            return restTemplate.getForObject(fineServiceBaseUrl + "/api/v1/fines/loan/" + loanId, FineDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void validateLoanRequest(ClientDTO client, ToolDTO tool, LocalDate dueDate, JwtAuthenticationToken principal) {
        if (dueDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Fecha inválida.");

        if (hasPendingFines(client.getId(), principal)) {
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

    private LoanWithFineInfoDTO buildLoanWithFineInfoDTO(LoanEntity loan) {
        LoanWithFineInfoDTO dto = new LoanWithFineInfoDTO();
        dto.setId(loan.getId());
        dto.setClientName(loan.getClientName());
        dto.setToolName(loan.getToolName());
        dto.setLoanDate(loan.getLoanDate());
        dto.setDueDate(loan.getDueDate());
        dto.setReturnDate(loan.getReturnDate());
        dto.setStatus(loan.getStatus());

        FineDTO fine = getFineByLoanId(loan.getId());
        if (fine != null) {
            dto.setFineId(fine.getId());
            dto.setFineAmount(fine.getAmount());
            dto.setFineStatus(fine.getStatus());
        }
        return dto;
    }

    // --- CLIENT ---

    private ClientDTO getClientById(Long id, JwtAuthenticationToken principal) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        ResponseEntity<ClientDTO> response = restTemplate.exchange(
                clientServiceBaseUrl + "/api/v1/clients/" + id,
                HttpMethod.GET,
                entity,
                ClientDTO.class
        );
        return response.getBody();
    }

    private ClientDTO getCurrentClient(JwtAuthenticationToken principal) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        ResponseEntity<ClientDTO> response = restTemplate.exchange(
                clientServiceBaseUrl + "/api/v1/clients/me",
                HttpMethod.GET,
                entity,
                ClientDTO.class
        );
        return response.getBody();
    }

    // --- TOOL ---

    private ToolDTO getToolById(Long id, JwtAuthenticationToken principal) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        ResponseEntity<ToolDTO> response = restTemplate.exchange(
                toolServiceBaseUrl + "/api/v1/tools/" + id,
                HttpMethod.GET,
                entity,
                ToolDTO.class
        );
        return response.getBody();
    }

    private void updateToolStatus(Long toolId, String newStatus, JwtAuthenticationToken principal) {
        String url = UriComponentsBuilder
                .fromHttpUrl(toolServiceBaseUrl + "/api/v1/tools/" + toolId + "/status")
                .queryParam("newStatus", newStatus)
                .toUriString();

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    // --- KARDEX ---

    private void sendToKardex(Long toolId, String toolName, String type, int qty, String user, JwtAuthenticationToken principal) {
        try {
            KardexDTO dto = new KardexDTO();
            dto.setToolId(toolId);
            dto.setToolName(toolName);
            dto.setMovementType(type);
            dto.setQuantityAffected(qty);
            dto.setUserResponsible(user);
            dto.setMovementDate(LocalDateTime.now());

            HttpEntity<KardexDTO> entity = new HttpEntity<>(dto, authHeaders(principal));
            restTemplate.exchange(
                    kardexServiceBaseUrl + "/api/kardex/movement",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
        } catch (Exception e) {
            System.err.println("Kardex error: " + e.getMessage());
        }
    }

    private HttpHeaders authHeaders(JwtAuthenticationToken principal) {
        HttpHeaders headers = new HttpHeaders();
        if (principal != null && principal.getToken() != null) {
            headers.setBearerAuth(principal.getToken().getTokenValue());
        }
        return headers;
    }

    // --- DTOs INTERNOS ---
    @Data
    public static class ClientDTO {
        private Long id;
        private String name;
        private String status;
        private String keycloakId;
    }

    @Data
    public static class ToolDTO {
        private Long id;
        private String name;
        private String status;
        private int availableStock;
    }

    @Data
    public static class KardexDTO {
        private Long toolId;
        private String toolName;
        private String movementType;
        private LocalDateTime movementDate;
        private int quantityAffected;
        private String userResponsible;
    }

    @Data
    public static class FineRequestDTO {
        private Long loanId;
        private Long clientId;
        private String type;
        private int amount;
    }

    @Data
    public static class FineDTO {
        private Long id;
        private int amount;
        private String status;
    }
}
