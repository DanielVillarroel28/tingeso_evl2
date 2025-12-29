package com.example.fineservice.repositories;

import com.example.fineservice.entities.ConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConfigurationRepository extends JpaRepository<ConfigurationEntity, Long> {

    Optional<ConfigurationEntity> findByConfigKey(String configKey);

}