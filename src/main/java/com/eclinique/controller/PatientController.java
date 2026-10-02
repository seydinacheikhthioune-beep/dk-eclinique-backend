package com.eclinique.controller;

import com.eclinique.model.Patient;
import com.eclinique.dto.PatientImportResult;
import com.eclinique.service.PatientService;
import com.eclinique.service.NotificationService;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.RendezVousService;
import com.eclinique.service.EncaissementService;
import com.eclinique.model.TypeEncaissement;
import com.eclinique.model.RendezVous;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PatientController {

    private final PatientService patientService;
    private final NotificationService notificationService;
    private final RendezVousService rendezVousService;
    private final EncaissementService encaissementService;

    @GetMapping
    public List<Patient> findAll(@RequestParam(required = false) String recherche) {
        if (recherche != null && !recherche.isBlank()) {
            return patientService.rechercher(recherche);
        }
        return patientService.findAll();
    }

    @GetMapping("/{id}")
    public Patient findById(@PathVariable Long id) {
        return patientService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','MEDECIN','INFIRMIER')")
    public ResponseEntity<Patient> create(@Valid @RequestBody Patient patient,
                                          @RequestParam(required = false) String suite,
                                          @RequestParam(required = false) Long medecinId,
                                          @RequestParam(required = false) String typeConsultation,
                                          @RequestParam(required = false) Double montant,
                                          @RequestParam(required = false) Double montantConsultation,
                                          @RequestParam(required = false) Double partOrganisme,
                                          @RequestParam(required = false) LocalDateTime dateHeure,
                                          @RequestParam(required = false, defaultValue = "30") Integer dureeMinutes,
                                          @RequestParam(required = false) String motif,
                                          @AuthenticationPrincipal UtilisateurPrincipal principal) {
        // "montantConsultation" est conservé pour compatibilité avec les anciens clients
        Double montantPercu = montant != null ? montant : montantConsultation;
        boolean rendezVous = "RENDEZVOUS".equalsIgnoreCase(suite);
        boolean consultation = "CONSULTATION".equalsIgnoreCase(suite);
        if ((rendezVous || consultation) && (montantPercu == null || montantPercu < 0)) {
            throw new IllegalArgumentException("Un montant positif est obligatoire pour une consultation ou un rendez-vous");
        }
        if (rendezVous && (dateHeure == null || medecinId == null)) {
            throw new IllegalArgumentException("La date, l'heure et le médecin sont obligatoires pour un rendez-vous");
        }
        if (partOrganisme != null && montantPercu != null && (partOrganisme < 0 || partOrganisme > montantPercu)) {
            throw new IllegalArgumentException("La part prise en charge doit être comprise entre 0 et le montant");
        }
        Patient cree = patientService.create(patient);
        boolean receptionniste = principal != null && principal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RECEPTIONNISTE".equals(authority.getAuthority()));
        if (rendezVous) {
            RendezVous rdv = rendezVousService.create(RendezVous.builder()
                    .patient(cree)
                    .medecin(com.eclinique.model.Utilisateur.builder().id(medecinId).build())
                    .dateHeure(dateHeure)
                    .dureeMinutes(dureeMinutes)
                    .motif(motif)
                    .build());
            encaissementService.enregistrer(cree, TypeEncaissement.RENDEZVOUS, montantPercu, medecinId,
                    null, rdv.getId(), partOrganisme, principal);
            notificationService.notifierNouveauPatient(cree, suite, medecinId, rdv.getId(), typeConsultation, montantPercu);
        } else if (consultation) {
            encaissementService.enregistrer(cree, TypeEncaissement.CONSULTATION, montantPercu, medecinId,
                    typeConsultation, null, partOrganisme, principal);
            notificationService.notifierNouveauPatient(cree, suite, medecinId, null, typeConsultation, montantPercu);
        } else if (receptionniste) {
            notificationService.notifierNouveauPatient(cree);
        }
        return ResponseEntity.ok(cree);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','MEDECIN','INFIRMIER')")
    public ResponseEntity<PatientImportResult> importer(@RequestParam("fichier") MultipartFile fichier) {
        return ResponseEntity.ok(patientService.importer(fichier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','MEDECIN','INFIRMIER')")
    public Patient update(@PathVariable Long id, @Valid @RequestBody Patient patient) {
        return patientService.update(id, patient);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
