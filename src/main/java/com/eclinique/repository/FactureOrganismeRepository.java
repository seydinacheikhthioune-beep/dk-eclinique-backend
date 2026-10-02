package com.eclinique.repository;

import com.eclinique.model.FactureOrganisme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FactureOrganismeRepository extends JpaRepository<FactureOrganisme, Long> {
    List<FactureOrganisme> findAllByOrderByDateEmissionDesc();
    List<FactureOrganisme> findByOrganismeIdOrderByDateEmissionDesc(Long organismeId);
    boolean existsByNumero(String numero);
}
