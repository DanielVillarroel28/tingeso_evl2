package com.example.toolservice.repositories;

import com.example.toolservice.entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface ToolRepository extends JpaRepository<ToolEntity, Long> {

}
