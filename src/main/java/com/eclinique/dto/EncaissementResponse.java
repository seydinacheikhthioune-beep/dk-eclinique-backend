package com.eclinique.dto;

import com.eclinique.model.Encaissement;
import com.eclinique.model.TypeEncaissement;

import java.time.LocalDateTime;

public record EncaissementResponse(Long id, TypeEncaissement type, double montant, Long patientId,
                                   String patientNom, String numeroDossier, String medecinNom,
                                   String typeConsultation, String enregistreParNom,
                                   LocalDateTime dateEncaissement, Long organismeId, String organismeNom,
                                   double partOrganisme, double partPatient, String matriculeAssure,
                                   Long factureOrganismeId) {

    public static EncaissementResponse of(Encaissement e) {
        return new EncaissementResponse(e.getId(), e.getType(), e.getMontant(), e.getPatientId(), e.getPatientNom(),
                e.getNumeroDossier(), e.getMedecinNom(), e.getTypeConsultation(), e.getEnregistreParNom(),
                e.getDateEncaissement(), e.getOrganismeId(), e.getOrganismeNom(), e.partOrganismeEffective(),
                e.partPatientEffective(), e.getMatriculeAssure(), e.getFactureOrganismeId());
    }
}
