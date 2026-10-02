package com.eclinique.service;

import com.eclinique.dto.BilanPeriodeResponse;
import com.eclinique.dto.ComptabiliteResponse;
import com.eclinique.dto.CreanceOrganismeResponse;
import com.eclinique.model.Encaissement;
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
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ComptabiliteService {
    private final FactureRepository factureRepository;
    private final EncaissementRepository encaissementRepository;
    private final PaiementEmployeRepository paiementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final OrganismeService organismeService;

    @Transactional(readOnly = true)
    public ComptabiliteResponse resume() {
        double consultations = value(factureRepository.sumConsultations());
        double hospitalisations = value(factureRepository.sumHospitalisations());
        // Toutes les factures émises hors annulées (y compris celles qui ne sont ni consultation ni hospitalisation)
        double factures = value(factureRepository.sumNonAnnulees());
        double encaissements = value(encaissementRepository.sumTotal());
        double encaissementsConsultations = value(encaissementRepository.sumByType(TypeEncaissement.CONSULTATION));
        double encaissementsRendezVous = value(encaissementRepository.sumByType(TypeEncaissement.RENDEZVOUS));
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        double encaissementsAujourdhui = value(encaissementRepository.sumBetween(debutJour, debutJour.plusDays(1)));
        double partPatients = value(encaissementRepository.sumPartPatient());
        double partOrganismes = value(encaissementRepository.sumPartOrganisme());
        List<CreanceOrganismeResponse> creances = organismeService.creances();
        double creancesEnAttente = creances.stream().mapToDouble(c -> c.nonFacture() + c.factureEnAttente()).sum();
        double creancesPayees = creances.stream().mapToDouble(CreanceOrganismeResponse::paye).sum();
        double recettes = factures + encaissements;
        double paiements = paiementRepository.findAll().stream().mapToDouble(PaiementEmploye::getMontant).sum();
        return new ComptabiliteResponse(factures, consultations, hospitalisations, encaissements,
                encaissementsConsultations, encaissementsRendezVous, encaissementsAujourdhui, partPatients,
                partOrganismes, creancesEnAttente, creancesPayees, recettes, paiements, recettes - paiements);
    }

    /**
     * Bilan découpé par période : MOIS (12 mois de l'année), TRIMESTRE (4), SEMESTRE (2)
     * ou ANNEE (les 5 années se terminant par {@code annee}).
     */
    @Transactional(readOnly = true)
    public List<BilanPeriodeResponse> bilan(String periode, int annee) {
        List<BilanPeriodeResponse> resultat = new ArrayList<>();
        switch (periode.toUpperCase(Locale.ROOT)) {
            case "MOIS" -> {
                for (int m = 1; m <= 12; m++) {
                    LocalDate debut = LocalDate.of(annee, m, 1);
                    String libelle = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.FRENCH).replace(".", "");
                    resultat.add(bilanPeriode(libelle.substring(0, 1).toUpperCase() + libelle.substring(1), debut, debut.plusMonths(1)));
                }
            }
            case "TRIMESTRE" -> {
                for (int t = 0; t < 4; t++) {
                    LocalDate debut = LocalDate.of(annee, t * 3 + 1, 1);
                    resultat.add(bilanPeriode("T" + (t + 1), debut, debut.plusMonths(3)));
                }
            }
            case "SEMESTRE" -> {
                for (int s = 0; s < 2; s++) {
                    LocalDate debut = LocalDate.of(annee, s * 6 + 1, 1);
                    resultat.add(bilanPeriode("S" + (s + 1), debut, debut.plusMonths(6)));
                }
            }
            case "ANNEE" -> {
                for (int a = annee - 4; a <= annee; a++) {
                    LocalDate debut = LocalDate.of(a, 1, 1);
                    resultat.add(bilanPeriode(String.valueOf(a), debut, debut.plusYears(1)));
                }
            }
            default -> throw new IllegalArgumentException("Période invalide : utilisez MOIS, TRIMESTRE, SEMESTRE ou ANNEE");
        }
        return resultat;
    }

    private BilanPeriodeResponse bilanPeriode(String libelle, LocalDate debut, LocalDate finExclue) {
        LocalDateTime de = debut.atStartOfDay();
        LocalDateTime a = finExclue.atStartOfDay();
        List<Encaissement> encaissements = encaissementRepository
                .findByDateEncaissementGreaterThanEqualAndDateEncaissementLessThan(de, a);
        double patients = encaissements.stream().mapToDouble(Encaissement::partPatientEffective).sum();
        double organismes = encaissements.stream().mapToDouble(Encaissement::partOrganismeEffective).sum();
        double factures = factureRepository.findNonAnnuleesBetween(de, a).stream()
                .mapToDouble(f -> f.getMontantTotal() == null ? 0 : f.getMontantTotal()).sum();
        double depenses = paiementRepository.findByDatePaiementGreaterThanEqualAndDatePaiementLessThan(de, a).stream()
                .mapToDouble(PaiementEmploye::getMontant).sum();
        double recettes = patients + organismes + factures;
        return new BilanPeriodeResponse(libelle, debut, finExclue.minusDays(1), patients, organismes, factures,
                recettes, depenses, recettes - depenses, encaissements.size());
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