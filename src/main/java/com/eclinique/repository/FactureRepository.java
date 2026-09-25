package com.eclinique.repository;

import com.eclinique.model.Facture;
import com.eclinique.model.StatutFacture;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    long countByConsultationIsNotNull();
    long countByDateAdmissionIsNotNull();
    @Query("select coalesce(sum(f.montantTotal), 0) from Facture f where f.consultation is not null")
    Double sumConsultations();
    @Query("select coalesce(sum(f.montantTotal), 0) from Facture f where f.dateAdmission is not null")
    Double sumHospitalisations();
    @Override
    @EntityGraph(attributePaths = {"patient", "lignes"})
    List<Facture> findAll();

    @Override
    @EntityGraph(attributePaths = {"patient", "lignes"})
    Optional<Facture> findById(Long id);

    Optional<Facture> findByNumeroFacture(String numeroFacture);
    @EntityGraph(attributePaths = {"patient", "lignes"})
    List<Facture> findByPatientId(Long patientId);
    @EntityGraph(attributePaths = {"patient", "lignes"})
    List<Facture> findByStatut(StatutFacture statut);

    @EntityGraph(attributePaths = {"patient", "lignes"})
    List<Facture> findByDateAdmissionIsNotNullOrderByDateAdmissionDesc();
}
