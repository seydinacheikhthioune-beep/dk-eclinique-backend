package com.eclinique.dto;

public record ComptabiliteResponse(double totalFactures, double totalConsultations,
                                   double totalHospitalisations, double totalPaiementsEmployes,
                                   double solde) {
}