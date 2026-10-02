package com.eclinique.dto;

import com.eclinique.model.TypeOrganisme;

/** Situation d'un organisme : ce qu'il doit à la clinique, facturé ou non. */
public record CreanceOrganismeResponse(Long organismeId, String nom, TypeOrganisme type, long nombrePrisesEnCharge,
                                       double totalPrisEnCharge, double nonFacture, double factureEnAttente,
                                       double paye) {
}
