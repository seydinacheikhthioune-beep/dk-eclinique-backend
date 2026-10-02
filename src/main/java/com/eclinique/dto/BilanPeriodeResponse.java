package com.eclinique.dto;

import java.time.LocalDate;

/**
 * Bilan d'une période. Les recettes sont comptées à la date de l'acte :
 * part payée par les patients + part due par les organismes + factures (hors annulées).
 */
public record BilanPeriodeResponse(String libelle, LocalDate debut, LocalDate fin, double recettesPatients,
                                   double recettesOrganismes, double recettesFactures, double totalRecettes,
                                   double depenses, double solde, long nombreActes) {
}
