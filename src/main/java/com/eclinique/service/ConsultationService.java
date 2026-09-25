package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.dto.ConsultationRequest;
import com.eclinique.dto.PrescriptionLigneRequest;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.*;
import com.eclinique.repository.ConsultationRepository;
import com.eclinique.repository.PatientRepository;
import com.eclinique.repository.RendezVousRepository;
import com.eclinique.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Gère le dossier médical (consultations) et déclenche automatiquement
 * la sortie de stock des médicaments prescrits via StockService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RendezVousRepository rendezVousRepository;
    private final StockService stockService;
    private final NotificationService notificationService;

    public List<Consultation> findAll() {
        return consultationRepository.findAllByOrderByDateConsultationDesc();
    }

    public List<Consultation> findByPatient(Long patientId) {
        return consultationRepository.findByPatientIdOrderByDateConsultationDesc(patientId);
    }

    public Consultation findById(Long id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation introuvable : " + id));
    }

    @Audite(action = TypeActionAudit.CREATION, entite = "Consultation")
    public Consultation creer(ConsultationRequest req, Long medecinConnecteId) {
        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable"));
        Utilisateur medecin = utilisateurRepository.findById(req.getMedecinId())
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable"));

        RendezVous rdv = null;
        if (req.getRendezVousId() != null) {
            rdv = rendezVousRepository.findById(req.getRendezVousId()).orElse(null);
        }

        Consultation consultation = Consultation.builder()
                .patient(patient)
                .medecin(medecin)
                .rendezVous(rdv)
                .type(req.getType() == null ? TypeConsultation.GENERALE : req.getType())
                .motif(req.getMotif())
                .symptomes(req.getSymptomes())
                .diagnostic(req.getDiagnostic())
                .observations(req.getObservations())
                .temperature(req.getTemperature())
                .tensionArterielle_systolique(req.getTensionSystolique())
                .tensionArterielle_diastolique(req.getTensionDiastolique())
                .poids(req.getPoids())
                .taille(req.getTaille())
                .prescriptions(new ArrayList<>())
                .build();

        Consultation sauvegardee = consultationRepository.save(consultation);

        // Ajout des prescriptions + décrémentation automatique du stock
        if (req.getPrescriptions() != null) {
            for (PrescriptionLigneRequest ligneReq : req.getPrescriptions()) {
                Medicament medicament = stockService.findMedicamentById(ligneReq.getMedicamentId());

                PrescriptionLigne ligne = PrescriptionLigne.builder()
                        .consultation(sauvegardee)
                        .medicament(medicament)
                        .quantite(ligneReq.getQuantite())
                        .posologie(ligneReq.getPosologie())
                        .dureeTraitement(ligneReq.getDureeTraitement())
                        .build();
                sauvegardee.getPrescriptions().add(ligne);

                // Gestion automatisée du stock : sortie déclenchée par la prescription
                stockService.sortieAutomatiquePourPrescription(medicament, ligneReq.getQuantite(), medecinConnecteId);
            }
        }

        // Si la consultation découle d'un rendez-vous, on le marque terminé
        if (rdv != null) {
            rdv.setStatut(StatutRendezVous.TERMINE);
            rendezVousRepository.save(rdv);
        }

        Consultation resultat = consultationRepository.save(sauvegardee);
        String libelle = resultat.getType() == TypeConsultation.SPECIALISEE ? "specialisee" : "generale";
        notificationService.notifierReceptionnistes(resultat.getId(), patient.getId(),
            "Une consultation " + libelle + " de " + patient.getPrenom() + " " + patient.getNom()
                + " est disponible pour impression.");
        return resultat;
    }
}
