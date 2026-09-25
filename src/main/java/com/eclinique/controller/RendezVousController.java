package com.eclinique.controller;

import com.eclinique.model.RendezVous;
import com.eclinique.model.StatutRendezVous;
import com.eclinique.service.RendezVousService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/rendezvous")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RendezVousController {

    private final RendezVousService rendezVousService;

    @GetMapping
    public List<RendezVous> findAll(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long medecinId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        if (patientId != null) return rendezVousService.findByPatient(patientId);
        if (medecinId != null) return rendezVousService.findByMedecin(medecinId);
        if (debut != null && fin != null) return rendezVousService.findEntrePeriodes(debut, fin);
        return rendezVousService.findAll();
    }

    @GetMapping("/{id}")
    public RendezVous findById(@PathVariable Long id) {
        return rendezVousService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','MEDECIN')")
    public ResponseEntity<RendezVous> create(@Valid @RequestBody RendezVous rendezVous) {
        return ResponseEntity.ok(rendezVousService.create(rendezVous));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','MEDECIN')")
    public RendezVous update(@PathVariable Long id, @RequestBody RendezVous rendezVous) {
        return rendezVousService.update(id, rendezVous);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','MEDECIN')")
    public RendezVous changerStatut(@PathVariable Long id, @RequestParam StatutRendezVous statut) {
        return rendezVousService.changerStatut(id, statut);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rendezVousService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
