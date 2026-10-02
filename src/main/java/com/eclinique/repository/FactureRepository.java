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
    @Query("select coalesce(sum(f.montantTotal), 0) from Facture f where f.statut <> com.eclinique.model.StatutFacture.ANNULEE")
    Double sumNonAnnulees();
    @Query("select f from Facture f where f.statut <> com.eclinique.model.StatutFacture.ANNULEE and f.dateFacture >= :debut and f.dateFacture < :fin")
    List<Facture> findNonAnnuleesBetween(java.time.LocalDateTime debut, java.time.LocalDateTime fin);
    @Override
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme", "lignes"})
    List<Facture> findAll();

    @Override
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme", "lignes"})
    Optional<Facture> findById(Long id);

    @Query("""
            select f from Facture f join fetch f.patient
            where f.organisme.id = :organismeId and f.factureOrganismeId is null and f.partOrganisme > 0
              and f.statut <> com.eclinique.model.StatutFacture.ANNULEE
              and f.dateFacture >= :debut and f.dateFacture < :fin
            order by f.dateFacture asc""")
    List<Facture> findAFacturer(Long organismeId, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme"})
    List<Facture> findByFactureOrganismeIdOrderByDateFactureAsc(Long factureOrganismeId);

    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme"})
    @Query("select f from Facture f where f.organisme is not null and f.statut <> com.eclinique.model.StatutFacture.ANNULEE")
    List<Facture> findTiersPayantNonAnnulees();

    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme"})
    List<Facture> findByDateFactureGreaterThanEqualAndDateFactureLessThanOrderByDateFactureAsc(
            java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    Optional<Facture> findByNumeroFacture(String numeroFacture);
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme", "lignes"})
    List<Facture> findByPatientId(Long patientId);
    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme", "lignes"})
    List<Facture> findByStatut(StatutFacture statut);

    @EntityGraph(attributePaths = {"patient", "patient.organisme", "organisme", "lignes"})
    List<Facture> findByDateAdmissionIsNotNullOrderByDateAdmissionDesc();
}
