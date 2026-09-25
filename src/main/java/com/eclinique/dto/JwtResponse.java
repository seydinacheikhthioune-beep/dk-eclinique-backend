package com.eclinique.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String nom;
    private String prenom;
    private String role;

    public JwtResponse(String token, Long id, String username, String nom, String prenom, String role) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.username = username;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
    }
}
