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

    Optional<FineEntity> findByLoanId(Long loanId);

    @Query("SELECT f FROM FineEntity f WHERE f.clientId = :clientId AND f.status = 'Pendiente'")
    List<FineEntity> findPendingFinesByClientId(@Param("clientId") Long clientId);

    List<FineEntity> findByClientKeycloakId(String clientKeycloakId);
}

