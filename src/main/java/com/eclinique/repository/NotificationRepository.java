package com.eclinique.repository;

import com.eclinique.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataireIdOrderByDateCreationDesc(Long destinataireId);
    long countByDestinataireIdAndLueFalse(Long destinataireId);
    boolean existsByDestinataireIdAndPatientIdAndType(Long destinataireId, Long patientId, String type);
    void deleteByPatientId(Long patientId);

    /** Suppression en masse, exécutée immédiatement (avant la suppression de l'utilisateur). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Notification n where n.destinataire.id = :destinataireId")
    void deleteByDestinataireId(@Param("destinataireId") Long destinataireId);
    boolean existsByDestinataireIdAndRendezVousIdAndType(Long destinataireId, Long rendezVousId, String type);
}