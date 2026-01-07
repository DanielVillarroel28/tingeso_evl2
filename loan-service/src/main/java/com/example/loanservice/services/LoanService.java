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

    //Listar Prestamos
    public List<LoanWithFineInfoDTO> getLoansWithFineInfo() {
        return loanRepository.findAll().stream()
                .map(this::buildLoanWithFineInfoDTO)
                .collect(Collectors.toList());
    }

    //Listar Prestamos para un usuario específico
    public List<LoanWithFineInfoDTO> getLoansForUser(String keycloakId) {
        return loanRepository.findByClientKeycloakId(keycloakId).stream()
                .map(this::buildLoanWithFineInfoDTO)
                .collect(Collectors.toList());
    }

    // Crear Préstamo
    @Transactional
    public LoanEntity createLoan(LoanDTO loanRequest, JwtAuthenticationToken principal) {
        ClientDTO clientDTO;
        if (loanRequest.getClientId() != null) {
            clientDTO = getClientById(loanRequest.getClientId(), principal);
        } else {
            if (principal == null) throw new IllegalArgumentException("Falta clientId o token JWT.");
            clientDTO = getCurrentClient(principal);
        }

        ToolDTO toolDTO = getToolById(loanRequest.getToolId(), principal);
        validateLoanRequest(clientDTO, toolDTO, loanRequest.getDueDate(), principal);
        updateToolStatus(toolDTO.getId(), "Prestada", principal);

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
        sendToKardex(savedLoan.getToolId(), savedLoan.getToolName(), "Préstamo", -1, savedLoan.getClientName(), principal);

        return savedLoan;
    }

    // Procesar Devolución
    @Transactional
    public LoanWithFineInfoDTO processReturn(Long loanId, ReturnRequestDTO returnRequest) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!"Activo".equals(loan.getStatus())) throw new RuntimeException("Este préstamo no está activo.");

        loan.setReturnDate(LocalDate.now());
        loan.setStatus("Devuelto");

        String damageType = returnRequest.getStatus();
        String finalToolStatus = "Disponible";
        boolean needsRestriction = false;

        if ("Irreparable".equals(damageType)) {
            finalToolStatus = "Dada de baja";
            needsRestriction = true;

            // Obtener precio de reposición de la herramienta
            int replacementValue = 0;
            try {
                ToolDTO toolInfo = getToolById(loan.getToolId(), null);
                if (toolInfo != null) replacementValue = toolInfo.getReplacementValue();
            } catch (Exception e) {
                System.err.println("No se pudo obtener el valor de reposición: " + e.getMessage());
            }

            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, "EMPLEADO", null);
            sendToKardex(loan.getToolId(), loan.getToolName(), "Baja", -1, "EMPLEADO", null);
            createFine(loan, "Daño Irreparable", replacementValue, null);

        } else if ("Dañada".equals(damageType)) {
            finalToolStatus = "En reparación";
            needsRestriction = true;
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, "EMPLEADO", null);
            sendToKardex(loan.getToolId(), loan.getToolName(), "Reparación", 0, "EMPLEADO", null);
            createFine(loan, "Daño Reparable", 0, null);
        } else {
            sendToKardex(loan.getToolId(), loan.getToolName(), "Devolución", 1, "EMPLEADO", null);
        }

        // Verificar Atraso
        if (loan.getReturnDate().isAfter(loan.getDueDate())) {
            createFine(loan, "Atraso", 0, null);
            needsRestriction = true;
        }

        if (needsRestriction) {
            updateClientStatus(loan.getClientId(), "Restringido", null);
        }

        updateToolStatus(loan.getToolId(), finalToolStatus, null);
        LoanEntity savedLoan = loanRepository.save(loan);
        return buildLoanWithFineInfoDTO(savedLoan);
    }

    private void updateClientStatus(Long clientId, String newStatus, JwtAuthenticationToken principal) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(clientServiceBaseUrl + "/api/v1/clients/" + clientId + "/status")
                    .queryParam("newStatus", newStatus)
                    .toUriString();

            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            System.out.println("Cliente " + clientId + " actualizado a " + newStatus + " exitosamente.");
        } catch (Exception e) {
            System.err.println("Error actualizando estado del cliente: " + e.getMessage());
        }
    }


    public LoanEntity updateLoan(LoanEntity loan) {
        return loanRepository.save(loan);
    }

    public boolean deleteLoan(Long id) throws Exception {
        LoanEntity loan = loanRepository.findById(id).orElseThrow(() -> new Exception("Préstamo no encontrado"));
        if (loan.getReturnDate() == null) throw new Exception("No se puede eliminar préstamo activo.");
        loanRepository.deleteById(id);
        return true;
    }


    private void createFine(LoanEntity loan, String type, int amountOverride, JwtAuthenticationToken principal) {
        try {
            FineRequestDTO fineReq = new FineRequestDTO();
            fineReq.setLoanId(loan.getId());
            fineReq.setClientId(loan.getClientId());
            fineReq.setClientName(loan.getClientName());
            fineReq.setClientKeycloakId(loan.getClientKeycloakId());
            fineReq.setToolName(loan.getToolName());
            fineReq.setType(type);
            fineReq.setAmount(amountOverride);

            if (loan.getReturnDate() != null && loan.getDueDate() != null) {
                long diff = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
                if (diff > 0) fineReq.setOverdueDays(diff);
            }

            HttpEntity<FineRequestDTO> entity = new HttpEntity<>(fineReq, authHeaders(principal));
            restTemplate.exchange(fineServiceBaseUrl + "/api/v1/fines", HttpMethod.POST, entity, Void.class);
        } catch (Exception e) {
            System.err.println("Error creando multa: " + e.getMessage());
        }
    }

    private boolean hasPendingFines(Long clientId, JwtAuthenticationToken principal) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    fineServiceBaseUrl + "/api/v1/fines/check-pending/" + clientId,
                    HttpMethod.GET, entity, Boolean.class);
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            return false;
        }
    }

    private FineDTO getFineByLoanId(Long loanId) {
        try {
            return restTemplate.getForObject(fineServiceBaseUrl + "/api/v1/fines/loan/" + loanId, FineDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void validateLoanRequest(ClientDTO client, ToolDTO tool, LocalDate dueDate, JwtAuthenticationToken principal) {
        if (dueDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Fecha inválida.");
        if (hasPendingFines(client.getId(), principal)) throw new RuntimeException("El cliente tiene multas impagas.");
        if (!"Disponible".equals(tool.getStatus()) || tool.getAvailableStock() < 1) throw new RuntimeException("Herramienta no disponible.");
        if (!"Activo".equals(client.getStatus())) throw new RuntimeException("Cliente restringido.");
        if (loanRepository.countByClientIdAndStatus(client.getId(), "Activo") >= 5) throw new RuntimeException("Límite de préstamos excedido.");
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

    private ClientDTO getClientById(Long id, JwtAuthenticationToken principal) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        return restTemplate.exchange(clientServiceBaseUrl + "/api/v1/clients/" + id, HttpMethod.GET, entity, ClientDTO.class).getBody();
    }

    private ClientDTO getCurrentClient(JwtAuthenticationToken principal) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        return restTemplate.exchange(clientServiceBaseUrl + "/api/v1/clients/me", HttpMethod.GET, entity, ClientDTO.class).getBody();
    }

    private ToolDTO getToolById(Long id, JwtAuthenticationToken principal) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        return restTemplate.exchange(toolServiceBaseUrl + "/api/v1/tools/" + id, HttpMethod.GET, entity, ToolDTO.class).getBody();
    }

    private void updateToolStatus(Long toolId, String newStatus, JwtAuthenticationToken principal) {
        String url = UriComponentsBuilder.fromHttpUrl(toolServiceBaseUrl + "/api/v1/tools/" + toolId + "/status")
                .queryParam("newStatus", newStatus).toUriString();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    private void sendToKardex(Long toolId, String toolName, String type, int qty, String user, JwtAuthenticationToken principal) {
        try {
            KardexDTO dto = new KardexDTO();
            dto.setToolId(toolId); dto.setToolName(toolName); dto.setMovementType(type);
            dto.setQuantityAffected(qty); dto.setUserResponsible(user); dto.setMovementDate(LocalDateTime.now());
            HttpEntity<KardexDTO> entity = new HttpEntity<>(dto, authHeaders(principal));
            restTemplate.exchange(kardexServiceBaseUrl + "/api/v1/kardex/movement", HttpMethod.POST, entity, Void.class);
        } catch (Exception e) { System.err.println("Kardex error: " + e.getMessage()); }
    }

    private HttpHeaders authHeaders(JwtAuthenticationToken principal) {
        HttpHeaders headers = new HttpHeaders();
        if (principal != null && principal.getToken() != null) {
            headers.setBearerAuth(principal.getToken().getTokenValue());
        }
        return headers;
    }

    //DTOs
    @Data public static class ClientDTO { private Long id; private String name; private String status; private String keycloakId; }
    @Data public static class ToolDTO { private Long id; private String name; private String status; private int availableStock; private int replacementValue; }
    @Data public static class KardexDTO { private Long toolId; private String toolName; private String movementType; private LocalDateTime movementDate; private int quantityAffected; private String userResponsible; }
    @Data public static class FineRequestDTO { private Long loanId; private Long clientId; private String clientName; private String clientKeycloakId; private String toolName; private String type; private int amount; private long overdueDays; }
    @Data public static class FineDTO { private Long id; private int amount; private String status; }
}