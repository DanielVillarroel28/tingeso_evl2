package com.example.fineservice.services;

import com.example.fineservice.entities.ConfigurationEntity;
import com.example.fineservice.repositories.ConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

    @Autowired
    private ConfigurationRepository configurationRepository;

    public int getFee(String key) {
        ConfigurationEntity config = configurationRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada: " + key));
        return Integer.parseInt(config.getConfigValue());
    }

    public ConfigurationEntity updateFee(String key, int newValue) {
        if (newValue < 0) {
            throw new IllegalArgumentException("El valor de la tarifa no puede ser negativo.");
        }

        ConfigurationEntity config = configurationRepository.findByConfigKey(key)
                .orElse(new ConfigurationEntity());

        config.setConfigKey(key);
        config.setConfigValue(String.valueOf(newValue));

        return configurationRepository.save(config);
    }
}
