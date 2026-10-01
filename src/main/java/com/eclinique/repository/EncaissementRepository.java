package com.eclinique.repository;

import com.eclinique.model.Encaissement;
import com.eclinique.model.TypeEncaissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface EncaissementRepository extends JpaRepository<Encaissement, Long> {
    List<Encaissement> findAllByOrderByDateEncaissementDesc();

    @Query("select coalesce(sum(e.montant), 0) from Encaissement e")
    Double sumTotal();

    @Query("select coalesce(sum(e.montant), 0) from Encaissement e where e.type = :type")
    Double sumByType(TypeEncaissement type);

    @Query("select coalesce(sum(e.montant), 0) from Encaissement e where e.dateEncaissement >= :debut and e.dateEncaissement < :fin")
    Double sumBetween(LocalDateTime debut, LocalDateTime fin);
}
