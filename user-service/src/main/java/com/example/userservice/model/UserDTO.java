package com.example.userservice.model;
import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String role; // Espera "ADMIN" o "EMPLEADO"
}