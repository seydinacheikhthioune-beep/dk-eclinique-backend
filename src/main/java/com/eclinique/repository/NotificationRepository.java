package com.eclinique.repository;

import com.eclinique.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataireIdOrderByDateCreationDesc(Long destinataireId);
    long countByDestinataireIdAndLueFalse(Long destinataireId);
    boolean existsByDestinataireIdAndPatientIdAndType(Long destinataireId, Long patientId, String type);
    boolean existsByDestinataireIdAndRendezVousIdAndType(Long destinataireId, Long rendezVousId, String type);
}