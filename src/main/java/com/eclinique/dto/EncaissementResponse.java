package com.eclinique.dto;

import com.eclinique.model.TypeEncaissement;

import java.time.LocalDateTime;

public record EncaissementResponse(Long id, TypeEncaissement type, double montant, Long patientId,
                                   String patientNom, String numeroDossier, String medecinNom,
                                   String typeConsultation, String enregistreParNom,
                                   LocalDateTime dateEncaissement) {
}
