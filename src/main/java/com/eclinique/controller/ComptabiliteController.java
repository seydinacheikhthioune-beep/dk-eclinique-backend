package com.eclinique.controller;

import com.eclinique.dto.ComptabiliteResponse;
import com.eclinique.dto.EncaissementResponse;
import com.eclinique.dto.PaiementEmployeRequest;
import com.eclinique.dto.PaiementEmployeResponse;
import com.eclinique.model.PaiementEmploye;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.ComptabiliteService;
import com.eclinique.service.EncaissementService;
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
    private final EncaissementService encaissementService;

    @GetMapping("/resume")
    public ComptabiliteResponse resume() { return service.resume(); }

    @GetMapping("/encaissements")
    public List<EncaissementResponse> encaissements() { return encaissementService.findAll(); }

    @GetMapping("/paiements")
    public List<PaiementEmployeResponse> paiements() { return service.paiements(); }

    @PostMapping("/paiements")
    public PaiementEmploye payer(@Valid @RequestBody PaiementEmployeRequest request,
                                 @AuthenticationPrincipal UtilisateurPrincipal auteur) {
        return service.payer(request, auteur);
    }
}