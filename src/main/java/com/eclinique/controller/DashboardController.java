package com.eclinique.controller;

import com.eclinique.model.StatutFacture;
import com.eclinique.model.StatutRendezVous;
import com.eclinique.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Fournit les indicateurs affichés sur le tableau de bord Angular. */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PatientRepository patientRepository;
    private final RendezVousRepository rendezVousRepository;
    private final FactureRepository factureRepository;
    private final MedicamentRepository medicamentRepository;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour = LocalDate.now().plusDays(1).atStartOfDay();

        stats.put("totalPatients", patientRepository.count());
        stats.put("rendezVousAujourdhui", rendezVousRepository.findByDateHeureBetween(debutJour, finJour).size());
        stats.put("rendezVousPlanifies", rendezVousRepository.findByStatut(StatutRendezVous.PLANIFIE).size());
        stats.put("facturesEnAttente", factureRepository.findByStatut(StatutFacture.EN_ATTENTE).size());
        stats.put("medicamentsEnAlerte", medicamentRepository.findMedicamentsEnAlerte().size());

        double chiffreAffaires = factureRepository.findByStatut(StatutFacture.PAYEE).stream()
                .mapToDouble(f -> f.getMontantTotal() == null ? 0 : f.getMontantTotal())
                .sum();
        stats.put("chiffreAffaires", chiffreAffaires);

        return stats;
    }
}
