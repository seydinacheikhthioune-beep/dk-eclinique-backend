package com.eclinique.controller;

import com.eclinique.dto.ApiResponse;
import com.eclinique.dto.MotDePasseRequest;
import com.eclinique.dto.ProfilRequest;
import com.eclinique.model.Utilisateur;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Compte de l'utilisateur connecté : consultation, modification et changement de mot de passe. */
@RestController
@RequestMapping("/profil")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfilController {
    private final UtilisateurService utilisateurService;

    @GetMapping
    public Utilisateur moi(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        return utilisateurService.findById(principal.getId());
    }

    @PutMapping
    public Utilisateur modifier(@AuthenticationPrincipal UtilisateurPrincipal principal,
                                @Valid @RequestBody ProfilRequest request) {
        return utilisateurService.modifierProfil(principal.getId(), request);
    }

    @PutMapping("/mot-de-passe")
    public ApiResponse changerMotDePasse(@AuthenticationPrincipal UtilisateurPrincipal principal,
                                         @Valid @RequestBody MotDePasseRequest request) {
        utilisateurService.changerMotDePasse(principal.getId(), request);
        return new ApiResponse(true, "Mot de passe modifié");
    }
}
