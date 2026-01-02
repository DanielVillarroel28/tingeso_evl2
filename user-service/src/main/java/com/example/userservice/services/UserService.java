package com.example.userservice.services;



import com.example.userservice.model.UserDTO;
import com.example.userservice.entities.UserEntity;
import com.example.userservice.repositories.UserRepository;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private UserRepository userRepository;

    @Value("${keycloak.realm}")
    private String realm;

    public String createUser(UserDTO userDTO) {
        // Validación básica
        if(userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("El usuario ya existe en la BD local");
        }

        // 1. Preparar usuario para Keycloak
        UserRepresentation user = new UserRepresentation();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEnabled(true);

        // 2. Configurar password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(userDTO.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        // 3. Crear en Keycloak
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);

        if (response.getStatus() == 201) {
            String path = response.getLocation().getPath();
            String userId = path.substring(path.lastIndexOf("/") + 1);

            // 4. Asignar Rol (RF7.2)
            try {
                // Busca el rol "ADMIN" o "EMPLEADO" en el realm
                RoleRepresentation realmRole = keycloak.realm(realm).roles()
                        .get(userDTO.getRole().toUpperCase()).toRepresentation();

                usersResource.get(userId).roles().realmLevel()
                        .add(Collections.singletonList(realmRole));
            } catch (Exception e) {
                // Si falla el rol, no abortamos la creación, pero lo logueamos
                System.err.println("Error asignando rol: " + e.getMessage());
            }

            // 5. Guardar en BD Local (Cumple requisito de BD propia)
            UserEntity localUser = new UserEntity();
            localUser.setUsername(userDTO.getUsername());
            localUser.setEmail(userDTO.getEmail());
            localUser.setFirstName(userDTO.getFirstName());
            localUser.setLastName(userDTO.getLastName());
            localUser.setKeycloakId(userId);
            userRepository.save(localUser);

            return "Usuario creado exitosamente con ID: " + userId;
        } else {
            // Manejo de errores si Keycloak rechaza (ej: email duplicado en Keycloak)
            throw new RuntimeException("Error creando usuario en Keycloak. Status: " + response.getStatus());
        }
    }
}