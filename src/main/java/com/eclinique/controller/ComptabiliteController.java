package com.eclinique.controller;

import com.eclinique.dto.ComptabiliteResponse;
import com.eclinique.dto.PaiementEmployeRequest;
import com.eclinique.dto.PaiementEmployeResponse;
import com.eclinique.model.PaiementEmploye;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.ComptabiliteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comptabilite")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ComptabiliteController {
    private final ComptabiliteService service;

    @GetMapping("/resume")
    public ComptabiliteResponse resume() { return service.resume(); }

    @GetMapping("/paiements")
    public List<PaiementEmployeResponse> paiements() { return service.paiements(); }

    @PostMapping("/paiements")
    public PaiementEmploye payer(@Valid @RequestBody PaiementEmployeRequest request,
                                 @AuthenticationPrincipal UtilisateurPrincipal auteur) {
        return service.payer(request, auteur);
    }
}