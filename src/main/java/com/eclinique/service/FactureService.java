package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.dto.FactureRequest;
import com.eclinique.dto.LigneFactureRequest;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.*;
import com.eclinique.repository.ConsultationRepository;
import com.eclinique.repository.FactureRepository;
import com.eclinique.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FactureService {

    private final FactureRepository factureRepository;
    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final NotificationService notificationService;

    public List<Facture> findAll() {
        return factureRepository.findAll().stream()
            .sorted(Comparator.comparing(Facture::getDateFacture,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public Facture findById(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + id));
    }

    public List<Facture> findByPatient(Long patientId) {
        return factureRepository.findByPatientId(patientId);
    }

    public List<Facture> findByStatut(StatutFacture statut) {
        return factureRepository.findByStatut(statut);
    }

    public List<Facture> findHospitalisations() {
        return factureRepository.findByDateAdmissionIsNotNullOrderByDateAdmissionDesc();
    }

    @Audite(action = TypeActionAudit.CREATION, entite = "Facture")
    public Facture creer(FactureRequest req) {
        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable"));

        Consultation consultation = null;
        if (req.getConsultationId() != null) {
            consultation = consultationRepository.findById(req.getConsultationId()).orElse(null);
        }

        Facture facture = Facture.builder()
                .numeroFacture(genererNumeroFacture())
                .patient(patient)
                .consultation(consultation)
                .observations(req.getObservations())
                .remise(req.getRemise())
                .modePaiement(req.getModePaiement())
                .lignes(new ArrayList<>())
                .build();

            if (req.getDateAdmission() != null) {
                LocalDate dateFin = req.getDateSortie() != null ? req.getDateSortie() : LocalDate.now();
                if (dateFin.isBefore(req.getDateAdmission())) {
                throw new IllegalArgumentException("La date de sortie doit être postérieure à la date d'admission");
                }
                int jours = Math.toIntExact(ChronoUnit.DAYS.between(req.getDateAdmission(), dateFin) + 1);
                double prixJournalier = req.getPrixJournalierHospitalisation() == null
                    ? 0.0 : req.getPrixJournalierHospitalisation();
                facture.setDateAdmission(req.getDateAdmission());
                facture.setDateSortie(req.getDateSortie());
                facture.setPrixJournalierHospitalisation(prixJournalier);
                facture.setJoursHospitalisation(jours);
            }

        double total = 0.0;
        if (req.getLignes() != null) {
            for (LigneFactureRequest l : req.getLignes()) {
                double montant = l.getQuantite() * l.getPrixUnitaire();
                LigneFacture ligne = LigneFacture.builder()
                        .facture(facture)
                        .designation(l.getDesignation())
                        .quantite(l.getQuantite())
                        .prixUnitaire(l.getPrixUnitaire())
                        .montant(montant)
                        .build();
                facture.getLignes().add(ligne);
                total += montant;
            }
        }
            if (facture.getJoursHospitalisation() != null) {
                double montantHospitalisation = facture.getJoursHospitalisation()
                    * facture.getPrixJournalierHospitalisation();
                LigneFacture ligneHospitalisation = LigneFacture.builder()
                    .facture(facture)
                    .designation("Hospitalisation (" + facture.getJoursHospitalisation() + " jour(s))")
                    .quantite(facture.getJoursHospitalisation())
                    .prixUnitaire(facture.getPrixJournalierHospitalisation())
                    .montant(montantHospitalisation)
                    .build();
                facture.getLignes().add(ligneHospitalisation);
                total += montantHospitalisation;
            }
        if (req.getRemise() != null) {
            total = total - req.getRemise();
        }
        facture.setMontantTotal(Math.max(total, 0));

        Facture resultat = factureRepository.save(facture);
        notificationService.notifierFacture(resultat.getId(), patient.getId(),
            "La facture " + resultat.getNumeroFacture() + " de " + patient.getPrenom() + " "
                + patient.getNom() + " est disponible pour impression.");
        return resultat;
    }

    @Audite(action = TypeActionAudit.PAIEMENT, entite = "Facture")
    public Facture marquerPayee(Long id, ModePaiement modePaiement) {
        Facture facture = findById(id);
        facture.setStatut(StatutFacture.PAYEE);
        facture.setModePaiement(modePaiement);
        facture.setDatePaiement(java.time.LocalDateTime.now());
        return factureRepository.save(facture);
    }

    @Audite(action = TypeActionAudit.ANNULATION, entite = "Facture")
    public Facture annuler(Long id) {
        Facture facture = findById(id);
        facture.setStatut(StatutFacture.ANNULEE);
        return factureRepository.save(facture);
    }

    @Audite(action = TypeActionAudit.SUPPRESSION, entite = "Facture")
    public void delete(Long id) {
        factureRepository.deleteById(id);
    }

    private String genererNumeroFacture() {
        long count = factureRepository.count() + 1;
        return "FACT-" + Year.now().getValue() + "-" + String.format("%05d", count);
    }
}
