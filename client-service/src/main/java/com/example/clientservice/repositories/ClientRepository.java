package com.example.clientservice.repositories;

import com.example.clientservice.entities.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

    public ClientEntity findByRut(String rut);

    Optional<ClientEntity> findByKeycloakId(String keycloakId);

    public List<ClientEntity> findByNameContainingIgnoreCase(String name);

    @Query(value = "SELECT * FROM clients WHERE clients.rut = :rut", nativeQuery = true)
    ClientEntity findByRutNativeQuery(@Param("rut") String rut);

}

