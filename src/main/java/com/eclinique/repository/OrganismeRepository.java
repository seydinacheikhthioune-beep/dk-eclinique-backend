package com.eclinique.repository;

import com.eclinique.model.Organisme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganismeRepository extends JpaRepository<Organisme, Long> {
    List<Organisme> findAllByOrderByNomAsc();
    List<Organisme> findByActifTrueOrderByNomAsc();
    boolean existsByNomIgnoreCase(String nom);
}
