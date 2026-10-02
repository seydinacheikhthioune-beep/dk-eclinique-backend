package com.eclinique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MotDePasseRequest {
    @NotBlank
    private String motDePasseActuel;
    @NotBlank
    @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères")
    private String nouveauMotDePasse;
}
