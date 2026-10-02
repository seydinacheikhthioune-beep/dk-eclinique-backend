package com.eclinique.dto;

import com.eclinique.model.Encaissement;
import com.eclinique.model.Facture;
import com.eclinique.model.Patient;
import com.eclinique.model.TypeEncaissement;

import java.time.LocalDateTime;

/**
 * Ligne d'une facture organisme : soit un encaissement d'accueil (consultation / rendez-vous),
 * soit une facture patient émise en tiers-payant.
 */
public record PriseEnChargeResponse(String source, Long id, String reference, LocalDateTime date, Long patientId,
                                    String patientNom, String numeroDossier, String matriculeAssure, String acte,
                                    String medecinNom, double montant, double partPatient, double partOrganisme) {

    public static PriseEnChargeResponse of(Encaissement e) {
        String acte = e.getType() == TypeEncaissement.RENDEZVOUS ? "Rendez-vous"
                : "SPECIALISEE".equals(e.getTypeConsultation()) ? "Consultation spécialisée" : "Consultation générale";
        return new PriseEnChargeResponse("ENCAISSEMENT", e.getId(), null, e.getDateEncaissement(), e.getPatientId(),
                e.getPatientNom(), e.getNumeroDossier(), e.getMatriculeAssure(), acte, e.getMedecinNom(),
                e.getMontant(), e.partPatientEffective(), e.partOrganismeEffective());
    }

    public static PriseEnChargeResponse of(Facture f) {
        Patient p = f.getPatient();
        String acte = f.getDateAdmission() != null ? "Hospitalisation" : "Facture de soins";
        return new PriseEnChargeResponse("FACTURE", f.getId(), f.getNumeroFacture(), f.getDateFacture(),
                p == null ? null : p.getId(), p == null ? null : p.getPrenom() + " " + p.getNom(),
                p == null ? null : p.getNumeroDossier(), f.getMatriculeAssure(), acte, null,
                f.getMontantTotal() == null ? 0 : f.getMontantTotal(), f.partPatientEffective(), f.partOrganismeEffective());
    }
}
