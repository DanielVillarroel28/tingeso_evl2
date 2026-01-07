package com.example.toolservice.controllers;

import com.example.toolservice.entities.ToolEntity;
import com.example.toolservice.services.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools") 
public class ToolController {

    @Autowired
    ToolService toolService;

    @GetMapping
    public ResponseEntity<List<ToolEntity>> listTools(jakarta.servlet.http.HttpServletRequest request) {
        System.out.println("PETICIÓN RECIBIDA");
        System.out.println("URL Real que llegó: " + request.getRequestURI());

        List<ToolEntity> tools = toolService.getTools();
        return ResponseEntity.ok(tools);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ToolEntity> getToolById(@PathVariable Long id) {
        ToolEntity tool = toolService.getToolById(id);
        return ResponseEntity.ok(tool);
    }

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

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {

        toolService.updateToolStatus(id, newStatus);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToolById(@PathVariable Long id) throws Exception {
        toolService.logicalDeleteTool(id);
        return ResponseEntity.noContent().build();
    }
}