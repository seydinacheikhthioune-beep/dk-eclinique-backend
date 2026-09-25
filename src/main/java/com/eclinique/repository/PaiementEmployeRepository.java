package com.eclinique.repository;

import com.eclinique.model.PaiementEmploye;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementEmployeRepository extends JpaRepository<PaiementEmploye, Long> {
    List<PaiementEmploye> findAllByOrderByDatePaiementDesc();
    List<PaiementEmploye> findByEmployeIdOrderByDatePaiementDesc(Long employeId);
}