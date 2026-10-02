package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.dto.FactureOrganismeRequest;
import com.eclinique.dto.FactureOrganismeResponse;
import com.eclinique.dto.PriseEnChargeResponse;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.*;
import com.eclinique.repository.EncaissementRepository;
import com.eclinique.repository.FactureOrganismeRepository;
import com.eclinique.repository.FactureRepository;
import com.eclinique.repository.OrganismeRepository;
import com.eclinique.repository.UtilisateurRepository;
import com.eclinique.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Stream;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FactureOrganismeService {
    private final FactureOrganismeRepository factureRepository;
    private final OrganismeRepository organismeRepository;
    private final EncaissementRepository encaissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FactureRepository facturePatientRepository;

    @Transactional(readOnly = true)
    public List<FactureOrganismeResponse> findAll(Long organismeId) {
        List<FactureOrganisme> factures = organismeId == null
                ? factureRepository.findAllByOrderByDateEmissionDesc()
                : factureRepository.findByOrganismeIdOrderByDateEmissionDesc(organismeId);
        Map<Long, Long> nombreLignes = Stream.concat(
                        encaissementRepository.findByOrganismeIdIsNotNull().stream().map(Encaissement::getFactureOrganismeId),
                        facturePatientRepository.findTiersPayantNonAnnulees().stream().map(Facture::getFactureOrganismeId))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        return factures.stream()
                .map(f -> FactureOrganismeResponse.of(f, List.of(), nombreLignes.getOrDefault(f.getId(), 0L).intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FactureOrganismeResponse findById(Long id) {
        FactureOrganisme facture = trouver(id);
        List<PriseEnChargeResponse> lignes = trier(Stream.concat(
                encaissementRepository.findByFactureOrganismeIdOrderByDateEncaissementAsc(id).stream().map(PriseEnChargeResponse::of),
                facturePatientRepository.findByFactureOrganismeIdOrderByDateFactureAsc(id).stream().map(PriseEnChargeResponse::of)));
        return FactureOrganismeResponse.of(facture, lignes, lignes.size());
    }

    /** Prises en charge pas encore facturées pour un organisme sur une période (aperçu avant création). */
    @Transactional(readOnly = true)
    public List<PriseEnChargeResponse> aFacturer(FactureOrganismeRequest req) {
        verifierPeriode(req);
        LocalDateTime debut = req.getPeriodeDebut().atStartOfDay();
        LocalDateTime fin = req.getPeriodeFin().plusDays(1).atStartOfDay();
        return trier(Stream.concat(
                encaissementRepository.findAFacturer(req.getOrganismeId(), debut, fin).stream().map(PriseEnChargeResponse::of),
                facturePatientRepository.findAFacturer(req.getOrganismeId(), debut, fin).stream().map(PriseEnChargeResponse::of)));
    }

    @Audite(action = TypeActionAudit.CREATION, entite = "FactureOrganisme")
    public FactureOrganismeResponse creer(FactureOrganismeRequest req, UtilisateurPrincipal auteur) {
        verifierPeriode(req);
        Organisme organisme = organismeRepository.findById(req.getOrganismeId())
                .orElseThrow(() -> new ResourceNotFoundException("Organisme introuvable"));
        LocalDateTime debut = req.getPeriodeDebut().atStartOfDay();
        LocalDateTime fin = req.getPeriodeFin().plusDays(1).atStartOfDay();
        List<Encaissement> prises = encaissementRepository.findAFacturer(organisme.getId(), debut, fin);
        List<Facture> facturesPatients = facturePatientRepository.findAFacturer(organisme.getId(), debut, fin);
        if (prises.isEmpty() && facturesPatients.isEmpty()) {
            throw new IllegalArgumentException("Aucune prise en charge à facturer pour " + organisme.getNom() + " sur cette période");
        }
        String creePar = auteur == null ? null : utilisateurRepository.findById(auteur.getId())
                .map(u -> u.getPrenom() + " " + u.getNom()).orElse(null);
        FactureOrganisme facture = factureRepository.save(FactureOrganisme.builder()
                .numero(genererNumero())
                .organisme(organisme)
                .periodeDebut(req.getPeriodeDebut())
                .periodeFin(req.getPeriodeFin())
                .montantTotal(prises.stream().mapToDouble(Encaissement::partOrganismeEffective).sum()
                        + facturesPatients.stream().mapToDouble(Facture::partOrganismeEffective).sum())
                .observations(req.getObservations())
                .creeParNom(creePar)
                .build());
        prises.forEach(e -> e.setFactureOrganismeId(facture.getId()));
        encaissementRepository.saveAll(prises);
        facturesPatients.forEach(f -> f.setFactureOrganismeId(facture.getId()));
        facturePatientRepository.saveAll(facturesPatients);
        return findById(facture.getId());
    }

    @Audite(action = TypeActionAudit.PAIEMENT, entite = "FactureOrganisme")
    public FactureOrganismeResponse marquerPayee(Long id, ModePaiement modePaiement) {
        FactureOrganisme facture = trouver(id);
        if (facture.getStatut() != StatutFacture.EN_ATTENTE) {
            throw new IllegalArgumentException("Seule une facture en attente peut être marquée payée");
        }
        facture.setStatut(StatutFacture.PAYEE);
        facture.setModePaiement(modePaiement);
        facture.setDatePaiement(LocalDateTime.now());
        factureRepository.save(facture);
        return findById(id);
    }

    /** Annule la facture et libère ses prises en charge pour qu'elles puissent être refacturées. */
    @Audite(action = TypeActionAudit.ANNULATION, entite = "FactureOrganisme")
    public FactureOrganismeResponse annuler(Long id) {
        FactureOrganisme facture = trouver(id);
        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new IllegalArgumentException("Une facture payée ne peut pas être annulée");
        }
        List<Encaissement> lignes = encaissementRepository.findByFactureOrganismeIdOrderByDateEncaissementAsc(id);
        lignes.forEach(e -> e.setFactureOrganismeId(null));
        encaissementRepository.saveAll(lignes);
        List<Facture> facturesPatients = facturePatientRepository.findByFactureOrganismeIdOrderByDateFactureAsc(id);
        facturesPatients.forEach(f -> f.setFactureOrganismeId(null));
        facturePatientRepository.saveAll(facturesPatients);
        facture.setStatut(StatutFacture.ANNULEE);
        factureRepository.save(facture);
        return FactureOrganismeResponse.of(facture, List.of(), 0);
    }

    private List<PriseEnChargeResponse> trier(Stream<PriseEnChargeResponse> lignes) {
        return lignes.sorted(Comparator.comparing(PriseEnChargeResponse::date,
                Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    private FactureOrganisme trouver(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture organisme introuvable : " + id));
    }

    private void verifierPeriode(FactureOrganismeRequest req) {
        if (req.getPeriodeFin().isBefore(req.getPeriodeDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }
    }

    private String genererNumero() {
        long count = factureRepository.count() + 1;
        String candidat;
        do {
            candidat = "FORG-" + Year.now().getValue() + "-" + String.format("%05d", count++);
        } while (factureRepository.existsByNumero(candidat));
        return candidat;
    }
}
