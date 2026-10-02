package com.eclinique.repository;

import com.eclinique.model.RendezVous;
import com.eclinique.model.StatutRendezVous;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
    @Override
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "medecin"})
    List<RendezVous> findAll();

    @Override
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "medecin"})
    java.util.Optional<RendezVous> findById(Long id);

    @EntityGraph(attributePaths = {"patient", "patient.organisme", "medecin"})
    List<RendezVous> findByPatientId(Long patientId);
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "medecin"})
    List<RendezVous> findByMedecinId(Long medecinId);
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "medecin"})
    List<RendezVous> findByStatut(StatutRendezVous statut);
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "medecin"})
    List<RendezVous> findByDateHeureBetween(LocalDateTime debut, LocalDateTime fin);
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "medecin"})
    List<RendezVous> findByMedecinIdAndDateHeureBetween(Long medecinId, LocalDateTime debut, LocalDateTime fin);
}
