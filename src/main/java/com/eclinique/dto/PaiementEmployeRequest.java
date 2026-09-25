package com.eclinique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaiementEmployeRequest {
    @NotNull private Long employeId;
    @NotNull private Double montant;
    @NotBlank private String periode;
    private String motif;
}