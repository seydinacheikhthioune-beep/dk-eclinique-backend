package com.eclinique.dto;

import com.eclinique.model.ModePaiement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.time.LocalDate;

@Data
public class FactureRequest {
    @NotNull
    private Long patientId;
    private Long consultationId;
    private String observations;
    @PositiveOrZero
    private Double remise;
    private LocalDate dateAdmission;
    private LocalDate dateSortie;
    @PositiveOrZero
    private Double prixJournalierHospitalisation;
    private ModePaiement modePaiement;
    @NotEmpty
    @Valid
    private List<LigneFactureRequest> lignes;
}
