package com.eclinique.dto;

public record ComptabiliteResponse(double totalFactures, double totalConsultations,
                                   double totalHospitalisations, double totalEncaissements,
                                   double encaissementsConsultations, double encaissementsRendezVous,
                                   double encaissementsAujourdhui, double totalPartPatients,
                                   double totalPartOrganismes, double creancesOrganismesEnAttente,
                                   double creancesOrganismesPayees, double totalRecettes,
                                   double totalPaiementsEmployes, double solde) {
}