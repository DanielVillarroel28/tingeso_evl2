package com.example.toolservice.services;

import com.example.toolservice.entities.ToolEntity;
import com.example.toolservice.repositories.ToolRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class ToolService {

    @Autowired
    ToolRepository toolRepository;

    @Autowired
    RestTemplate restTemplate;

    // Inyección de la URL igual que en LoanService
    @Value("${services.kardex.base-url:http://kardex-service:8080}")
    private String kardexServiceBaseUrl;

    // Guardar herramienta con estado inicial y kardex
    public ToolEntity saveTool(ToolEntity tool) {
        tool.setStatus("Disponible");

        if (tool.getStateInitial() == null || tool.getStateInitial().isEmpty()) {
            tool.setStateInitial("NUEVA");
        }

        // Asignar stock por defecto
        if (tool.getAvailableStock() == 0) tool.setAvailableStock(1);

        ToolEntity newTool = toolRepository.save(tool);

        // Enviamos el movimiento al Kardex
        sendToKardex(newTool, "Ingreso", 1, "ADMIN");

        return newTool;
    }

    public boolean logicalDeleteTool(Long id) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Herramienta no encontrada"));

        tool.setStatus("Dada de baja");
        toolRepository.save(tool);

        sendToKardex(tool, "Baja", -1, "ADMIN");

        return true;
    }

    private void sendToKardex(ToolEntity tool, String type, int quantity, String user) {
        try {
            // Creamos el DTO (usando la clase interna definida abajo)
            KardexDTO dto = new KardexDTO();
            dto.setToolId(tool.getId());
            dto.setToolName(tool.getName());
            dto.setMovementType(type);
            dto.setMovementDate(LocalDateTime.now());
            dto.setQuantityAffected(quantity);
            dto.setUserResponsible(user);

            String url = kardexServiceBaseUrl + "/api/v1/kardex/movement";

            // Usamos authHeaders() para obtener el token automáticamente
            HttpEntity<KardexDTO> entity = new HttpEntity<>(dto, authHeaders());

            // Enviamos usando exchange
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            System.out.println("Movimiento enviado a Kardex exitosamente: " + type);

        } catch (Exception e) {
            System.err.println("Error comunicando con Kardex: " + e.getMessage());
        }
    }

    // Helper para obtener los headers con el Token
    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String token = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null) {
                    headers.set(HttpHeaders.AUTHORIZATION, token);
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo obtener el token de seguridad: " + e.getMessage());
        }
        return headers;
    }

    public ToolEntity getToolById(Long id) {
        return toolRepository.findById(id).orElse(null);
    }

    public ToolEntity updateTool(Long id, ToolEntity tool) {
        return toolRepository.save(tool);
    }

    public boolean deleteTool(Long id) throws Exception {
        try {
            toolRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    // Actualizar estado
    public void updateToolStatus(Long id, String newStatus) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Herramienta no encontrada con id: " + id));
        String decodedStatus = URLDecoder.decode(newStatus, StandardCharsets.UTF_8);

        tool.setStatus(decodedStatus);
        toolRepository.save(tool);
    }

    public ArrayList<ToolEntity> getTools() {
        return (ArrayList<ToolEntity>) toolRepository.findAll();
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
}