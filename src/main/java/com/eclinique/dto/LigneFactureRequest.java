package com.eclinique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class LigneFactureRequest {
    @NotBlank
    private String designation;
    @NotNull
    @Positive
    private Integer quantite;
    @NotNull
    @PositiveOrZero
    private Double prixUnitaire;
}
