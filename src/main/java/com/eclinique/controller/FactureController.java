package com.eclinique.controller;

import com.eclinique.dto.FactureRequest;
import com.eclinique.model.Facture;
import com.eclinique.model.ModePaiement;
import com.eclinique.model.StatutFacture;
import com.eclinique.service.FactureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/factures")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FactureController {

    private final FactureService factureService;

    @GetMapping
    public List<Facture> findAll(@RequestParam(required = false) Long patientId,
                                  @RequestParam(required = false) StatutFacture statut) {
        if (patientId != null) return factureService.findByPatient(patientId);
        if (statut != null) return factureService.findByStatut(statut);
        return factureService.findAll();
    }

    @GetMapping("/hospitalisations")
    public List<Facture> findHospitalisations() {
        return factureService.findHospitalisations();
    }

    @GetMapping("/{id}")
    public Facture findById(@PathVariable Long id) {
        return factureService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','MEDECIN')")
    public ResponseEntity<Facture> creer(@Valid @RequestBody FactureRequest request) {
        return ResponseEntity.ok(factureService.creer(request));
    }

    @PatchMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public Facture payer(@PathVariable Long id, @RequestParam ModePaiement modePaiement) {
        return factureService.marquerPayee(id, modePaiement);
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public Facture annuler(@PathVariable Long id) {
        return factureService.annuler(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        factureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
