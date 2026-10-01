package com.eclinique.service;

import com.eclinique.dto.ComptabiliteResponse;
import com.eclinique.dto.PaiementEmployeRequest;
import com.eclinique.dto.PaiementEmployeResponse;
import com.eclinique.model.PaiementEmploye;
import com.eclinique.model.Utilisateur;
import com.eclinique.model.TypeEncaissement;
import com.eclinique.repository.EncaissementRepository;
import com.eclinique.repository.FactureRepository;
import com.eclinique.repository.PaiementEmployeRepository;
import com.eclinique.repository.UtilisateurRepository;
import com.eclinique.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComptabiliteService {
    private final FactureRepository factureRepository;
    private final EncaissementRepository encaissementRepository;
    private final PaiementEmployeRepository paiementRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public ComptabiliteResponse resume() {
        double consultations = value(factureRepository.sumConsultations());
        double hospitalisations = value(factureRepository.sumHospitalisations());
        double factures = consultations + hospitalisations;
        double encaissements = value(encaissementRepository.sumTotal());
        double encaissementsConsultations = value(encaissementRepository.sumByType(TypeEncaissement.CONSULTATION));
        double encaissementsRendezVous = value(encaissementRepository.sumByType(TypeEncaissement.RENDEZVOUS));
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        double encaissementsAujourdhui = value(encaissementRepository.sumBetween(debutJour, debutJour.plusDays(1)));
        double recettes = factures + encaissements;
        double paiements = paiementRepository.findAll().stream().mapToDouble(PaiementEmploye::getMontant).sum();
        return new ComptabiliteResponse(factures, consultations, hospitalisations, encaissements,
                encaissementsConsultations, encaissementsRendezVous, encaissementsAujourdhui, recettes,
                paiements, recettes - paiements);
    }

    @Transactional(readOnly = true)
    public List<PaiementEmployeResponse> paiements() {
        return paiementRepository.findAllByOrderByDatePaiementDesc().stream().map(p ->
                new PaiementEmployeResponse(p.getId(), p.getEmploye().getId(),
                        p.getEmploye().getPrenom() + " " + p.getEmploye().getNom(), p.getMontant(),
                        p.getPeriode(), p.getMotif(), p.getDatePaiement(),
                        p.getEffectuePar() == null ? null : p.getEffectuePar().getId())).toList();
    }

    public PaiementEmploye payer(PaiementEmployeRequest request, UtilisateurPrincipal auteur) {
        Utilisateur employe = utilisateurRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new IllegalArgumentException("Employe introuvable"));
        Utilisateur effectuePar = utilisateurRepository.findById(auteur.getId()).orElse(null);
        return paiementRepository.save(PaiementEmploye.builder()
                .employe(employe).montant(request.getMontant()).periode(request.getPeriode())
                .motif(request.getMotif()).effectuePar(effectuePar).build());
    }

    private double value(Double value) { return value == null ? 0 : value; }
}