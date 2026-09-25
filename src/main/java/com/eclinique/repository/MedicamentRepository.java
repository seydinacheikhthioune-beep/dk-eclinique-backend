package com.eclinique.repository;

import com.eclinique.model.Medicament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicamentRepository extends JpaRepository<Medicament, Long> {
    Optional<Medicament> findByCode(String code);
    boolean existsByCode(String code);
    List<Medicament> findByNomContainingIgnoreCase(String nom);

    // Alerte de stock : quantité <= seuil
    @org.springframework.data.jpa.repository.Query(
        "SELECT m FROM Medicament m WHERE m.quantiteStock <= m.seuilAlerte")
    List<Medicament> findMedicamentsEnAlerte();

    @org.springframework.data.jpa.repository.Query(
        "SELECT m FROM Medicament m WHERE m.dateExpiration IS NOT NULL AND m.dateExpiration <= :date")
    List<Medicament> findMedicamentsBientotPerimes(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);
}
