package com.eclinique.controller;

import com.eclinique.model.StatutFacture;
import com.eclinique.model.StatutRendezVous;
import com.eclinique.model.TypeEncaissement;
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
    private final EncaissementRepository encaissementRepository;

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
        double encaissements = valeur(encaissementRepository.sumTotal());
        // CA encaissé = factures payées + montants perçus à l'accueil (consultations / rendez-vous)
        stats.put("chiffreAffaires", chiffreAffaires + encaissements);
        stats.put("encaissementsTotal", encaissements);
        stats.put("encaissementsAujourdhui", valeur(encaissementRepository.sumBetween(debutJour, finJour)));
        stats.put("encaissementsConsultations", valeur(encaissementRepository.sumByType(TypeEncaissement.CONSULTATION)));
        stats.put("encaissementsRendezVous", valeur(encaissementRepository.sumByType(TypeEncaissement.RENDEZVOUS)));

        return stats;
    }

    private double valeur(Double montant) { return montant == null ? 0 : montant; }
}
