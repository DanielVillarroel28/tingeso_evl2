package com.example.toolservice.controllers;

import com.example.toolservice.entities.ToolEntity;
import com.example.toolservice.services.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools") // Ruta base
// @CrossOrigin("*") <--- ELIMINA ESTO. El Gateway ya maneja el CORS.
public class ToolController {

    @Autowired
    ToolService toolService;

    // CAMBIO 1: Quita el "/"
    @GetMapping
    public ResponseEntity<List<ToolEntity>> listTools() {
        List<ToolEntity> tools = toolService.getTools();
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ToolEntity> getToolById(@PathVariable Long id) {
        ToolEntity tool = toolService.getToolById(id);
        return ResponseEntity.ok(tool);
    }

    // CAMBIO 2: Quita el "/"
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ToolEntity> saveTool(@RequestBody ToolEntity tool) {
        ToolEntity toolNew = toolService.saveTool(tool);
        return ResponseEntity.ok(toolNew);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ToolEntity> updateTool(@PathVariable Long id, @RequestBody ToolEntity tool) {
        ToolEntity toolUpdated = toolService.updateTool(id, tool);
        return ResponseEntity.ok(toolUpdated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToolById(@PathVariable Long id) throws Exception {
        toolService.logicalDeleteTool(id);
        return ResponseEntity.noContent().build();
    }
}