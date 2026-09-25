package com.eclinique.repository;

import com.eclinique.model.Consultation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    @EntityGraph(attributePaths = {"patient", "medecin", "prescriptions", "prescriptions.medicament"})
    List<Consultation> findAllByOrderByDateConsultationDesc();

    @EntityGraph(attributePaths = {"patient", "medecin", "prescriptions", "prescriptions.medicament"})
    List<Consultation> findByPatientIdOrderByDateConsultationDesc(Long patientId);

    @Override
    @EntityGraph(attributePaths = {"patient", "medecin", "prescriptions", "prescriptions.medicament"})
    java.util.Optional<Consultation> findById(Long id);
    List<Consultation> findByMedecinId(Long medecinId);
}
