package com.eclinique.service;

import com.eclinique.dto.NotificationResponse;
import com.eclinique.model.Notification;
import com.eclinique.model.Role;
import com.eclinique.model.Utilisateur;
import com.eclinique.model.Patient;
import com.eclinique.repository.NotificationRepository;
import com.eclinique.repository.UtilisateurRepository;
import com.eclinique.repository.PatientRepository;
import com.eclinique.repository.ConsultationRepository;
import com.eclinique.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final RendezVousRepository rendezVousRepository;

    public void notifierReceptionnistes(Long consultationId, Long patientId, String message) {
        List<Utilisateur> receptionnistes = utilisateurRepository.findByRole(Role.RECEPTIONNISTE);
        receptionnistes.stream().filter(Utilisateur::isActif).forEach(receptionniste ->
                notificationRepository.save(Notification.builder()
                        .destinataire(receptionniste)
                        .message(message)
                        .consultationId(consultationId)
                        .patientId(patientId)
                        .type("CONSULTATION")
                        .build()));
                }

                public void notifierFacture(Long factureId, Long patientId, String message) {
                List<Utilisateur> receptionnistes = utilisateurRepository.findByRole(Role.RECEPTIONNISTE);
                receptionnistes.stream().filter(Utilisateur::isActif).forEach(receptionniste ->
                    notificationRepository.save(Notification.builder()
                        .destinataire(receptionniste)
                        .message(message)
                        .factureId(factureId)
                        .patientId(patientId)
                        .type("FACTURE")
                        .build()));
    }

    public void notifierNouveauPatient(Patient patient, String suite, Long medecinId) {
        notifierNouveauPatient(patient, suite, medecinId, null);
    }

    public void notifierNouveauPatient(Patient patient) {
        Map<Long, Utilisateur> destinataires = new LinkedHashMap<>();
        utilisateurRepository.findByRole(Role.ADMIN).stream()
            .filter(Utilisateur::isActif)
            .forEach(utilisateur -> destinataires.put(utilisateur.getId(), utilisateur));
        utilisateurRepository.findByRole(Role.RECEPTIONNISTE).stream()
            .filter(Utilisateur::isActif)
            .forEach(utilisateur -> destinataires.put(utilisateur.getId(), utilisateur));
        utilisateurRepository.findByRole(Role.MEDECIN).stream()
            .filter(Utilisateur::isActif)
            .forEach(utilisateur -> destinataires.put(utilisateur.getId(), utilisateur));

        destinataires.values().forEach(destinataire -> {
            if (!notificationRepository.existsByDestinataireIdAndPatientIdAndType(
                    destinataire.getId(), patient.getId(), "PATIENT_CONSULTATION")) {
                notificationRepository.save(Notification.builder()
                    .destinataire(destinataire)
                    .message("Ticket : nouveau patient " + patient.getPrenom() + " " + patient.getNom()
                        + " enregistré, en attente de prise en charge.")
                    .patientId(patient.getId())
                    .typeConsultation("GENERALE")
                    .montantConsultation(5000.0)
                    .type("PATIENT_CONSULTATION")
                    .build());
            }
        });
    }

    public void notifierNouveauPatient(Patient patient, String suite, Long medecinId, Long rendezVousId) {
        notifierNouveauPatient(patient, suite, medecinId, rendezVousId, null, null);
    }

    public void notifierNouveauPatient(Patient patient, String suite, Long medecinId, Long rendezVousId,
                                       String typeConsultation, Double montantConsultation) {
    Map<Long, Utilisateur> destinataires = new LinkedHashMap<>();
    utilisateurRepository.findByRole(Role.ADMIN).stream()
        .filter(Utilisateur::isActif)
        .forEach(utilisateur -> destinataires.put(utilisateur.getId(), utilisateur));
    if (medecinId != null) {
        utilisateurRepository.findById(medecinId)
            .filter(Utilisateur::isActif)
            .ifPresent(utilisateur -> destinataires.put(utilisateur.getId(), utilisateur));
    }
    utilisateurRepository.findByRole(Role.RECEPTIONNISTE).stream()
        .filter(Utilisateur::isActif)
        .forEach(utilisateur -> destinataires.put(utilisateur.getId(), utilisateur));
    String type = "CONSULTATION".equalsIgnoreCase(suite)
        ? "PATIENT_CONSULTATION" : "PATIENT_RENDEZVOUS";
    String libelle = "CONSULTATION".equalsIgnoreCase(suite) ? "consultation" : "rendez-vous";
    Utilisateur medecin = medecinId == null ? null : utilisateurRepository.findById(medecinId).orElse(null);
    destinataires.values().forEach(destinataire -> {
        boolean ticketDejaCree = rendezVousId != null
            ? notificationRepository.existsByDestinataireIdAndRendezVousIdAndType(destinataire.getId(), rendezVousId, type)
            : notificationRepository.existsByDestinataireIdAndPatientIdAndType(destinataire.getId(), patient.getId(), type);
        if (!ticketDejaCree) {
        notificationRepository.save(Notification.builder()
            .destinataire(destinataire)
            .message("Ticket : nouveau patient " + patient.getPrenom() + " " + patient.getNom()
                + " en attente pour un " + libelle + ".")
            .patientId(patient.getId())
                        .medecinId(medecinId)
                        .medecinNom(medecin != null ? medecin.getNom() : null)
                        .medecinPrenom(medecin != null ? medecin.getPrenom() : null)
                        .typeConsultation(typeConsultation)
                        .montantConsultation(montantConsultation)
                        .rendezVousId(rendezVousId)
            .type(type)
            .build());
        }
    });
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> pourUtilisateur(Long utilisateurId) {
        return notificationRepository.findByDestinataireIdOrderByDateCreationDesc(utilisateurId)
            .stream()
            .sorted(Comparator.comparing(Notification::getDateCreation,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Long utilisateurId) {
        return notificationRepository.countByDestinataireIdAndLueFalse(utilisateurId);
    }

    public void marquerLue(Long notificationId, Long utilisateurId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable"));
        if (!notification.getDestinataire().getId().equals(utilisateurId)) {
            throw new IllegalArgumentException("Cette notification ne vous appartient pas");
        }
        notification.setLue(true);
    }

    private NotificationResponse toResponse(Notification notification) {
        Patient patient = patientRepository.findById(notification.getPatientId()).orElse(null);
        Long medecinId = notification.getMedecinId();
        String medecinNom = notification.getMedecinNom();
        String medecinPrenom = notification.getMedecinPrenom();
        String typeConsultation = notification.getTypeConsultation();
        Double montantConsultation = notification.getMontantConsultation();

        if (medecinId == null && patient != null) {
            var consultation = consultationRepository.findByPatientIdOrderByDateConsultationDesc(patient.getId())
                    .stream().findFirst().orElse(null);
            if (consultation != null && consultation.getMedecin() != null) {
                medecinId = consultation.getMedecin().getId();
                medecinNom = consultation.getMedecin().getNom();
                medecinPrenom = consultation.getMedecin().getPrenom();
                typeConsultation = consultation.getType() != null ? consultation.getType().name() : typeConsultation;
            }
        }
        if (medecinId == null && notification.getRendezVousId() != null) {
            var rendezVous = rendezVousRepository.findById(notification.getRendezVousId()).orElse(null);
            if (rendezVous != null && rendezVous.getMedecin() != null) {
                medecinId = rendezVous.getMedecin().getId();
                medecinNom = rendezVous.getMedecin().getNom();
                medecinPrenom = rendezVous.getMedecin().getPrenom();
            }
        }
        if (typeConsultation == null) typeConsultation = "GENERALE";
        if (montantConsultation == null || montantConsultation <= 0) montantConsultation = 5000.0;
        return new NotificationResponse(notification.getId(), notification.getMessage(), notification.getType(),
            notification.getConsultationId(), notification.getFactureId(), notification.getRendezVousId(), notification.getPatientId(),
            patient != null ? patient.getNom() : null, patient != null ? patient.getPrenom() : null,
            patient != null ? patient.getNumeroDossier() : null, medecinId, medecinNom,
            medecinPrenom, typeConsultation, montantConsultation,
            notification.isLue(), notification.getDateCreation());
    }
}