package com.eclinique.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FactureOrganismeRequest {
    @NotNull(message = "L'organisme est obligatoire")
    private Long organismeId;
    @NotNull(message = "La date de début est obligatoire")
    private LocalDate periodeDebut;
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate periodeFin;
    private String observations;
}
