package com.eclinique.controller;

import com.eclinique.dto.FactureOrganismeRequest;
import com.eclinique.dto.FactureOrganismeResponse;
import com.eclinique.dto.PriseEnChargeResponse;
import com.eclinique.model.ModePaiement;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.FactureOrganismeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Factures adressées aux assureurs et IPM d'entreprise. */
@RestController
@RequestMapping("/factures-organismes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
public class FactureOrganismeController {
    private final FactureOrganismeService service;

    @GetMapping
    public List<FactureOrganismeResponse> findAll(@RequestParam(required = false) Long organismeId) {
        return service.findAll(organismeId);
    }

    @GetMapping("/{id}")
    public FactureOrganismeResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping("/apercu")
    public List<PriseEnChargeResponse> apercu(@Valid @RequestBody FactureOrganismeRequest request) {
        return service.aFacturer(request);
    }

    @PostMapping
    public FactureOrganismeResponse creer(@Valid @RequestBody FactureOrganismeRequest request,
                                          @AuthenticationPrincipal UtilisateurPrincipal auteur) {
        return service.creer(request, auteur);
    }

    @PatchMapping("/{id}/payer")
    public FactureOrganismeResponse payer(@PathVariable Long id, @RequestParam ModePaiement modePaiement) {
        return service.marquerPayee(id, modePaiement);
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    public FactureOrganismeResponse annuler(@PathVariable Long id) { return service.annuler(id); }
}
