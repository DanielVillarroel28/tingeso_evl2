package com.example.reportservice.services;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    // Conexión a LoanService (M2)
    @Value("${services.loan.base-url:http://loan-service:8080}")
    private String loanServiceBaseUrl;

    // --- REPORTE 1: Préstamos Activos (Vigentes y Atrasados) ---
    public List<LoanDTO> getActiveLoans() {
        List<LoanDTO> allLoans = fetchAllLoans();

        // Filtramos solo los que NO han sido devueltos (status Activo o similar)
        return allLoans.stream()
                .filter(loan -> "Activo".equalsIgnoreCase(loan.getStatus()) || loan.getReturnDate() == null)
                .map(loan -> {
                    // Calculamos estado visual en tiempo real
                    if (loan.getDueDate().isBefore(LocalDate.now())) {
                        loan.setStatus("Atrasado");
                    } else {
                        loan.setStatus("Vigente");
                    }
                    return loan;
                })
                .collect(Collectors.toList());
    }

    // --- REPORTE 2: Clientes con Atrasos ---
    public List<OverdueClientDTO> getOverdueClients() {
        List<LoanDTO> allLoans = fetchAllLoans();

        // 1. Filtramos préstamos que están vencidos y no devueltos
        List<LoanDTO> overdueLoans = allLoans.stream()
                .filter(loan -> loan.getReturnDate() == null && loan.getDueDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        // 2. Agrupamos por Cliente ID
        Map<Long, List<LoanDTO>> loansByClient = overdueLoans.stream()
                .collect(Collectors.groupingBy(LoanDTO::getClientId));

        // 3. Mapeamos a DTO de reporte
        return loansByClient.entrySet().stream()
                .map(entry -> {
                    Long clientId = entry.getKey();
                    List<LoanDTO> loans = entry.getValue();
                    LoanDTO first = loans.get(0); // Para sacar nombre/rut

                    OverdueClientDTO dto = new OverdueClientDTO();
                    dto.setId(clientId);
                    dto.setName(first.getClientName()); // Asumiendo que LoanDTO trae el nombre
                    dto.setRut("N/A"); // Si Loan no trae RUT, habría que llamar a ClientService

                    // Sumar días de atraso
                    long totalDays = loans.stream()
                            .mapToLong(l -> java.time.temporal.ChronoUnit.DAYS.between(l.getDueDate(), LocalDate.now()))
                            .sum();

                    dto.setTotalOverdueDays(totalDays);
                    dto.setPendingFinesCount(loans.size()); // 1 multa potencial por préstamo
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // --- REPORTE 3: Ranking de Herramientas ---
    public List<TopToolDTO> getTopTools() {
        List<LoanDTO> allLoans = fetchAllLoans();

        // Agrupamos por nombre de herramienta y contamos
        Map<String, Long> countMap = allLoans.stream()
                .collect(Collectors.groupingBy(LoanDTO::getToolName, Collectors.counting()));

        // Ordenamos descendente y limitamos top 10
        return countMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(entry -> new TopToolDTO(entry.getKey(), "General", entry.getValue()))
                .collect(Collectors.toList());
    }

    // --- MÉTODO AUXILIAR PARA LLAMAR A LOAN-SERVICE ---
    private List<LoanDTO> fetchAllLoans() {
        try {
            String url = loanServiceBaseUrl + "/api/v1/loans"; // Asumiendo que este endpoint devuelve todos

            HttpHeaders headers = new HttpHeaders();
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String token = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null) headers.set(HttpHeaders.AUTHORIZATION, token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<LoanDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<LoanDTO>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error obteniendo préstamos: " + e.getMessage());
            return List.of();
        }
    }

    // --- DTOs INTERNOS ---
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
}