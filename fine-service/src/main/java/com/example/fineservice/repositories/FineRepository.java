package com.example.fineservice.repositories;


import com.example.fineservice.entities.FineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<FineEntity, Long> {

    // Busca multas asociadas a un préstamo específico (usado por M2)
    Optional<FineEntity> findByLoanId(Long loanId);

    // Busca multas pendientes de un cliente (usado para validar préstamos)
    @Query("SELECT f FROM FineEntity f WHERE f.clientId = :clientId AND f.status = 'Pendiente'")
    List<FineEntity> findPendingFinesByClientId(@Param("clientId") Long clientId);

    // Busca por Keycloak ID (usado para "Mis Multas")
    List<FineEntity> findByClientKeycloakId(String clientKeycloakId);
}

