package com.eclinique.dto;

import com.eclinique.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UtilisateurRequest {
    @NotBlank
    private String username;
    private String password;
    @Email
    private String email;
    private String nom;
    private String prenom;
    @Pattern(regexp = "^$|\\+?[0-9][0-9 .()\\-]{7,20}$", message = "Le numéro de téléphone est invalide")
    private String telephone;
    private String profession;
    private Integer anneesExperience;
    private Double tauxHoraire;
    private Role role;
    private Boolean actif;
}
