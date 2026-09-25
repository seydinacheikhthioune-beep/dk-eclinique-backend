package com.eclinique.repository;

import com.eclinique.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByNumeroDossier(String numeroDossier);
    Optional<Patient> findByEmail(String email);

    List<Patient> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);

    boolean existsByNumeroDossier(String numeroDossier);
}
