package com.eclinique.repository;

import com.eclinique.model.Encaissement;
import com.eclinique.model.TypeEncaissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface EncaissementRepository extends JpaRepository<Encaissement, Long> {
    List<Encaissement> findAllByOrderByDateEncaissementDesc();

    List<Encaissement> findByDateEncaissementGreaterThanEqualAndDateEncaissementLessThan(LocalDateTime debut, LocalDateTime fin);

    List<Encaissement> findByOrganismeIdIsNotNull();

    List<Encaissement> findByFactureOrganismeIdOrderByDateEncaissementAsc(Long factureOrganismeId);

    @Query("""
            select e from Encaissement e
            where e.organismeId = :organismeId and e.factureOrganismeId is null and e.partOrganisme > 0
              and e.dateEncaissement >= :debut and e.dateEncaissement < :fin
            order by e.dateEncaissement asc""")
    List<Encaissement> findAFacturer(Long organismeId, LocalDateTime debut, LocalDateTime fin);

    @Query("select coalesce(sum(e.montant), 0) from Encaissement e")
    Double sumTotal();

    @Query("select coalesce(sum(e.montant), 0) from Encaissement e where e.type = :type")
    Double sumByType(TypeEncaissement type);

    @Query("select coalesce(sum(e.montant), 0) from Encaissement e where e.dateEncaissement >= :debut and e.dateEncaissement < :fin")
    Double sumBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("select coalesce(sum(coalesce(e.partPatient, e.montant)), 0) from Encaissement e")
    Double sumPartPatient();

    @Query("select coalesce(sum(e.partOrganisme), 0) from Encaissement e")
    Double sumPartOrganisme();
}
