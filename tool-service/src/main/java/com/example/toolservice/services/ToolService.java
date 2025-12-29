package com.example.toolservice.services;

import com.example.toolservice.entities.ToolEntity;
import com.example.toolservice.model.KardexDTO; // Importar el DTO creado arriba
import com.example.toolservice.repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class ToolService {

    @Autowired
    ToolRepository toolRepository;

    @Autowired
    RestTemplate restTemplate; // Asegúrate de tener el Bean configurado (ver abajo)

    public ToolEntity saveTool(ToolEntity tool) {
        // 1. Guardar la herramienta en la BD local de M1
        tool.setStatus("Disponible");
        // Asignar valores por defecto si vienen nulos para evitar errores
        if (tool.getAvailableStock() == 0) tool.setAvailableStock(1);

        ToolEntity newTool = toolRepository.save(tool);

        // 2. Comunicar el movimiento al microservicio Kardex (M5)
        sendToKardex(newTool, "Ingreso", 1, "ADMIN"); // "ADMIN" debería venir del token

        return newTool;
    }

    public boolean logicalDeleteTool(Long id) {
        ToolEntity tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Herramienta no encontrada"));

        tool.setStatus("Dada de baja");
        toolRepository.save(tool);

        // Comunicar la baja al Kardex
        sendToKardex(tool, "Baja", -1, "ADMIN");

        return true;
    }

    // Método auxiliar para evitar repetir código de conexión
    private void sendToKardex(ToolEntity tool, String type, int quantity, String user) {
        try {
            KardexDTO dto = new KardexDTO();
            dto.setToolId(tool.getId());
            dto.setToolName(tool.getName());
            dto.setMovementType(type);
            dto.setMovementDate(LocalDateTime.now());
            dto.setQuantityAffected(quantity);
            dto.setUserResponsible(user);

            // IMPORTANTE: Aquí se usa el nombre del servicio en Kubernetes/Eureka
            // Suponiendo que en el application.properties del M5 pusiste: spring.application.name=kardex-service
            String url = "http://kardex-service/api/kardex/movement";

            restTemplate.postForObject(url, dto, Void.class);

        } catch (Exception e) {
            // Manejar error: Podrías guardar en una tabla de "intentos fallidos"
            // o simplemente loguear que el kardex no respondió.
            System.err.println("Error comunicando con Kardex: " + e.getMessage());
        }
    }

    public ToolEntity getToolById(Long id) {
        return toolRepository.findById(id).get();
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

    public ArrayList<ToolEntity> getTools() {
        return (ArrayList<ToolEntity>) toolRepository.findAll();
    }
}

