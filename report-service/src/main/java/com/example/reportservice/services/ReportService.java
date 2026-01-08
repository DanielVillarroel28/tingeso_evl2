package com.example.reportservice.services;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.loan.base-url:http://loan-service:8080}")
    private String loanServiceBaseUrl;

    @Value("${services.client.base-url:http://client-service:8080}")
    private String clientServiceBaseUrl;

    //Ver prestamos activos
    public List<LoanDTO> getActiveLoans() {
        JwtAuthenticationToken principal = getCurrentPrincipal();
        List<LoanDTO> allLoans = fetchAllLoans(principal);

        return allLoans.stream()
                .filter(loan -> "Activo".equalsIgnoreCase(loan.getStatus()) || loan.getReturnDate() == null)
                .map(loan -> {
                    if (loan.getDueDate().isBefore(LocalDate.now())) {
                        loan.setStatus("Atrasado");
                    } else {
                        loan.setStatus("Vigente");
                    }
                    return loan;
                })
                .collect(Collectors.toList());
    }

    //Clientes con atraso
    public List<OverdueClientDTO> getOverdueClients() {
        JwtAuthenticationToken principal = getCurrentPrincipal();
        List<LoanDTO> allLoans = fetchAllLoans(principal);

        List<LoanDTO> overdueLoans = allLoans.stream()
                .filter(loan -> loan.getReturnDate() == null && loan.getDueDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        Map<Long, List<LoanDTO>> loansByClient = overdueLoans.stream()
                .collect(Collectors.groupingBy(LoanDTO::getClientId));

        return loansByClient.entrySet().stream()
                .map(entry -> {
                    Long clientId = entry.getKey();
                    List<LoanDTO> loans = entry.getValue();
                    LoanDTO first = loans.get(0);

                    // Obtener datos del cliente
                    ClientDTO clientDTO = getClientById(clientId, principal);

                    OverdueClientDTO dto = new OverdueClientDTO();
                    dto.setId(clientId);
                    dto.setName(clientDTO != null ? clientDTO.getName() : first.getClientName());
                    dto.setRut(clientDTO != null ? clientDTO.getRut() : "N/A");

                    long totalDays = loans.stream()
                            .mapToLong(l -> java.time.temporal.ChronoUnit.DAYS.between(l.getDueDate(), LocalDate.now()))
                            .sum();

                    dto.setTotalOverdueDays(totalDays);
                    dto.setPendingFinesCount(loans.size());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    //Ranking de herramienta
    public List<TopToolDTO> getTopTools() {
        JwtAuthenticationToken principal = getCurrentPrincipal();
        List<LoanDTO> allLoans = fetchAllLoans(principal);

        Map<String, Long> countMap = allLoans.stream()
                .collect(Collectors.groupingBy(LoanDTO::getToolName, Collectors.counting()));

        return countMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(entry -> new TopToolDTO(entry.getKey(), "General", entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<LoanDTO> fetchAllLoans(JwtAuthenticationToken principal) {
        try {
            String url = loanServiceBaseUrl + "/api/v1/loans";
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));

            ResponseEntity<List<LoanDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<LoanDTO>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error obteniendo préstamos: " + e.getMessage());
            return List.of();
        }
    }

    private ClientDTO getClientById(Long id, JwtAuthenticationToken principal) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(principal));
            ResponseEntity<ClientDTO> response = restTemplate.exchange(
                    clientServiceBaseUrl + "/api/v1/clients/" + id,
                    HttpMethod.GET,
                    entity,
                    ClientDTO.class
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error obteniendo cliente: " + e.getMessage());
            return null;
        }
    }

    private JwtAuthenticationToken getCurrentPrincipal() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object principal = attrs.getRequest().getUserPrincipal();
                if (principal instanceof JwtAuthenticationToken) {
                    return (JwtAuthenticationToken) principal;
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo principal: " + e.getMessage());
        }
        return null;
    }

    private HttpHeaders authHeaders(JwtAuthenticationToken principal) {
        HttpHeaders headers = new HttpHeaders();
        if (principal != null && principal.getToken() != null) {
            headers.setBearerAuth(principal.getToken().getTokenValue());
        }
        return headers;
    }

    // --- DTOs ---
    @Data
    public static class LoanDTO {
        private Long id;
        private Long clientId;
        private String clientName;
        private String toolName;
        private LocalDate loanDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private String status;
    }

    @Data
    public static class OverdueClientDTO {
        private Long id;
        private String name;
        private String rut;
        private long totalOverdueDays;
        private int pendingFinesCount;
    }

    @Data
    public static class TopToolDTO {
        private String toolName;
        private String category;
        private Long loanCount;

        public TopToolDTO(String toolName, String category, Long loanCount) {
            this.toolName = toolName;
            this.category = category;
            this.loanCount = loanCount;
        }
    }

    @Data
    public static class ClientDTO {
        private Long id;
        private String name;
        private String rut;
        private String status;
        private String keycloakId;
    }
}