package com.eclinique.dto;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String message, String type, Long consultationId, Long factureId,
                                   Long rendezVousId, Long patientId, String patientNom, String patientPrenom,
                                   String numeroDossier, Long medecinId, String medecinNom, String medecinPrenom,
                                   String typeConsultation, Double montantConsultation,
                                   boolean lue, LocalDateTime dateCreation) {
}