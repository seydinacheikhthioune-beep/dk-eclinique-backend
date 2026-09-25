package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.exception.BusinessException;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.*;
import com.eclinique.repository.PatientRepository;
import com.eclinique.repository.RendezVousRepository;
import com.eclinique.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional
public class RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final PatientRepository patientRepository;
    private final UtilisateurRepository utilisateurRepository;

    public List<RendezVous> findAll() {
        return rendezVousRepository.findAll().stream()
            .sorted(Comparator.comparing(RendezVous::getDateHeure,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public RendezVous findById(Long id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable : " + id));
    }

    public List<RendezVous> findByPatient(Long patientId) {
        return rendezVousRepository.findByPatientId(patientId);
    }

    public List<RendezVous> findByMedecin(Long medecinId) {
        return rendezVousRepository.findByMedecinId(medecinId);
    }

    public List<RendezVous> findEntrePeriodes(LocalDateTime debut, LocalDateTime fin) {
        return rendezVousRepository.findByDateHeureBetween(debut, fin);
    }

    @Audite(action = TypeActionAudit.CREATION, entite = "RendezVous")
    public RendezVous create(RendezVous rdv) {
        Patient patient = patientRepository.findById(rdv.getPatient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable"));
        Utilisateur medecin = utilisateurRepository.findById(rdv.getMedecin().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Médecin introuvable"));

        verifierDisponibilite(medecin.getId(), rdv.getDateHeure(), rdv.getDureeMinutes() == null ? 30 : rdv.getDureeMinutes(), null);

        rdv.setPatient(patient);
        rdv.setMedecin(medecin);
        if (rdv.getStatut() == null) rdv.setStatut(StatutRendezVous.PLANIFIE);
        if (rdv.getDureeMinutes() == null) rdv.setDureeMinutes(30);
        return rendezVousRepository.save(rdv);
    }

    @Audite(action = TypeActionAudit.MODIFICATION, entite = "RendezVous")
    public RendezVous update(Long id, RendezVous donnees) {
        RendezVous rdv = findById(id);
        if (donnees.getDateHeure() != null) {
            verifierDisponibilite(rdv.getMedecin().getId(), donnees.getDateHeure(),
                    donnees.getDureeMinutes() == null ? rdv.getDureeMinutes() : donnees.getDureeMinutes(), id);
            rdv.setDateHeure(donnees.getDateHeure());
        }
        if (donnees.getDureeMinutes() != null) rdv.setDureeMinutes(donnees.getDureeMinutes());
        if (donnees.getMotif() != null) rdv.setMotif(donnees.getMotif());
        if (donnees.getStatut() != null) rdv.setStatut(donnees.getStatut());
        if (donnees.getNotes() != null) rdv.setNotes(donnees.getNotes());
        return rendezVousRepository.save(rdv);
    }

    @Audite(action = TypeActionAudit.MODIFICATION, entite = "RendezVous")
    public RendezVous changerStatut(Long id, StatutRendezVous statut) {
        RendezVous rdv = findById(id);
        rdv.setStatut(statut);
        return rendezVousRepository.save(rdv);
    }

    @Audite(action = TypeActionAudit.SUPPRESSION, entite = "RendezVous")
    public void delete(Long id) {
        rendezVousRepository.deleteById(id);
    }

    /** Empêche deux rendez-vous qui se chevauchent pour le même médecin. */
    private void verifierDisponibilite(Long medecinId, LocalDateTime debut, int dureeMinutes, Long rdvIdAExclure) {
        LocalDateTime fin = debut.plusMinutes(dureeMinutes);
        List<RendezVous> existants = rendezVousRepository.findByMedecinIdAndDateHeureBetween(
                medecinId, debut.minusHours(6), fin.plusHours(6));

        boolean conflit = existants.stream()
                .filter(r -> rdvIdAExclure == null || !r.getId().equals(rdvIdAExclure))
                .filter(r -> r.getStatut() != StatutRendezVous.ANNULE)
                .anyMatch(r -> {
                    LocalDateTime rDebut = r.getDateHeure();
                    LocalDateTime rFin = rDebut.plusMinutes(r.getDureeMinutes() == null ? 30 : r.getDureeMinutes());
                    return debut.isBefore(rFin) && rDebut.isBefore(fin);
                });

        if (conflit) {
            throw new BusinessException("Le médecin a déjà un rendez-vous sur ce créneau horaire");
        }
    }
}
