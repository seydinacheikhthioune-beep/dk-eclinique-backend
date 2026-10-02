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
    /** false pour facturer tout au patient même s'il est assuré ; null/true applique le tiers-payant. */
    private Boolean tiersPayant;
    /** Part organisme saisie à la main ; null = calculée avec le taux du patient. */
    @PositiveOrZero
    private Double partOrganisme;
    @NotEmpty
    @Valid
    private List<LigneFactureRequest> lignes;
}
