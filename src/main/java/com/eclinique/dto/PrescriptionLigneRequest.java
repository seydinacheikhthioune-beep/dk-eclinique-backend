package com.eclinique.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrescriptionLigneRequest {
    @NotNull
    private Long medicamentId;
    @NotNull
    private Integer quantite;
    private String posologie;
    private String dureeTraitement;
}
