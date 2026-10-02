package com.eclinique.repository;

import com.eclinique.model.FactureOrganisme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FactureOrganismeRepository extends JpaRepository<FactureOrganisme, Long> {
    List<FactureOrganisme> findAllByOrderByDateEmissionDesc();
    List<FactureOrganisme> findByOrganismeIdOrderByDateEmissionDesc(Long organismeId);
    boolean existsByNumero(String numero);
    List<FactureOrganisme> findByDateEmissionGreaterThanEqualAndDateEmissionLessThanOrderByDateEmissionAsc(
            java.time.LocalDateTime debut, java.time.LocalDateTime fin);
    List<FactureOrganisme> findByStatutAndDateEnvoiIsNullOrderByDateEmissionAsc(com.eclinique.model.StatutFacture statut);
}
