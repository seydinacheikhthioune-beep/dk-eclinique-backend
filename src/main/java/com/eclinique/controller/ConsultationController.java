package com.eclinique.controller;

import com.eclinique.dto.ConsultationRequest;
import com.eclinique.model.Consultation;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consultations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ConsultationController {

    private final ConsultationService consultationService;

    @GetMapping
    public List<Consultation> findAll() {
        return consultationService.findAll();
    }

    @GetMapping("/patient/{patientId}")
    public List<Consultation> findByPatient(@PathVariable Long patientId) {
        return consultationService.findByPatient(patientId);
    }

    @GetMapping("/{id}")
    public Consultation findById(@PathVariable Long id) {
        return consultationService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDECIN')")
    public ResponseEntity<Consultation> creer(@Valid @RequestBody ConsultationRequest request,
                                               @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return ResponseEntity.ok(consultationService.creer(request, principal.getId()));
    }
}
