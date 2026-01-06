package com.example.toolservice.services;

import com.example.toolservice.entities.ToolEntity;
import com.example.toolservice.model.KardexDTO;
import com.example.toolservice.repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    // --- GUARDAR HERRAMIENTA (SOLUCIÓN 1: ESTADO INICIAL) ---
    public ToolEntity saveTool(ToolEntity tool) {
        // 1. Estado operativo actual
        tool.setStatus("Disponible");

        // 2. Estado Físico Inicial (Agregado para cumplir tu requerimiento)
        // Asumiendo que tu entidad tiene el campo 'stateInitial' mapeado a 'state_initial'
        // Si tu campo en la entidad se llama diferente, ajusta este setter.
        if (tool.getStateInitial() == null || tool.getStateInitial().isEmpty()) {
            tool.setStateInitial("NUEVA");
        }

        // Asignar stock por defecto
        if (tool.getAvailableStock() == 0) tool.setAvailableStock(1);

        ToolEntity newTool = toolRepository.save(tool);

        // 3. Kardex
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

    // Método auxiliar corregido
    private void sendToKardex(ToolEntity tool, String type, int quantity, String user) {
        try {
            KardexDTO dto = new KardexDTO();
            dto.setToolId(tool.getId());
            dto.setToolName(tool.getName());
            dto.setMovementType(type);
            dto.setMovementDate(LocalDateTime.now());
            dto.setQuantityAffected(quantity);
            dto.setUserResponsible(user);

            // CAMBIO: Usar Gateway (localhost:8080) y la ruta con /v1
            // Asegúrate que el Gateway tenga StripPrefix=1 para KARDEX-SERVICE
            String url = "http://localhost:8080/KARDEX-SERVICE/api/v1/kardex/movement";

            restTemplate.postForObject(url, dto, Void.class);

        } catch (Exception e) {
            System.err.println("Error comunicando con Kardex: " + e.getMessage());
        }
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

    // --- ACTUALIZAR ESTADO (SOLUCIÓN 2: DECODIFICAR URL) ---
    public void updateToolStatus(Long id, String newStatus) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Herramienta no encontrada con id: " + id));

        // AQUÍ ESTÁ EL TRUCO: Decodificamos el texto antes de guardar.
        // Esto transforma "Dada%20de%20baja" -> "Dada de baja"
        // y "En%20reparaci%C3%B3n" -> "En reparación"
        String decodedStatus = URLDecoder.decode(newStatus, StandardCharsets.UTF_8);

        tool.setStatus(decodedStatus);
        toolRepository.save(tool);
    }

    public ArrayList<ToolEntity> getTools() {
        return (ArrayList<ToolEntity>) toolRepository.findAll();
    }
}