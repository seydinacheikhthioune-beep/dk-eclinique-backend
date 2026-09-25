package com.eclinique.controller;

import com.eclinique.dto.ApiResponse;
import com.eclinique.dto.JwtResponse;
import com.eclinique.dto.LoginRequest;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Point de sortie explicite, appelé par le frontend avant de supprimer le jeton JWT côté client,
     * afin que la déconnexion soit tracée dans le journal d'audit (le JWT reste stateless côté serveur).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        if (principal != null) {
            authService.logout(principal);
        }
        return ResponseEntity.ok(new ApiResponse(true, "Déconnexion enregistrée"));
    }
}
