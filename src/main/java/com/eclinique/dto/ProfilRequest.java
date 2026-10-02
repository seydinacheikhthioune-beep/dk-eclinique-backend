package com.eclinique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Informations qu'un utilisateur connecté peut modifier sur son propre compte. */
@Data
public class ProfilRequest {
    @NotBlank
    private String nom;
    @NotBlank
    private String prenom;
    @Email
    private String email;
    @Pattern(regexp = "^$|\\+?[0-9][0-9 .()\\-]{7,20}$", message = "Le numéro de téléphone est invalide")
    private String telephone;
}
