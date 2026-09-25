package com.eclinique.repository;

import com.eclinique.model.MouvementStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {
    List<MouvementStock> findByMedicamentIdOrderByDateMouvementDesc(Long medicamentId);
    List<MouvementStock> findAllByOrderByDateMouvementDesc();
}
