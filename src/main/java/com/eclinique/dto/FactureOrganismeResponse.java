package com.eclinique.dto;

import com.eclinique.model.FactureOrganisme;
import com.eclinique.model.ModePaiement;
import com.eclinique.model.Organisme;
import com.eclinique.model.StatutFacture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FactureOrganismeResponse(Long id, String numero, Organisme organisme, LocalDate periodeDebut,
                                       LocalDate periodeFin, LocalDateTime dateEmission, double montantTotal,
                                       StatutFacture statut, ModePaiement modePaiement, LocalDateTime datePaiement,
                                       String observations, String creeParNom, int nombreLignes,
                                       List<PriseEnChargeResponse> lignes) {

    public static FactureOrganismeResponse of(FactureOrganisme f, List<PriseEnChargeResponse> lignes, int nombreLignes) {
        return new FactureOrganismeResponse(f.getId(), f.getNumero(), f.getOrganisme(), f.getPeriodeDebut(),
                f.getPeriodeFin(), f.getDateEmission(), f.getMontantTotal(), f.getStatut(), f.getModePaiement(),
                f.getDatePaiement(), f.getObservations(), f.getCreeParNom(), nombreLignes, lignes);
    }
}
