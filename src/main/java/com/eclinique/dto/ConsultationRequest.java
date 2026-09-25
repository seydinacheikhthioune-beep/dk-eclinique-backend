package com.eclinique.dto;

import java.util.List;

import com.eclinique.model.TypeConsultation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsultationRequest {
    @NotNull
    private Long patientId;
    @NotNull
    private Long medecinId;
    private Long rendezVousId;
    private TypeConsultation type = TypeConsultation.GENERALE;
    private String motif;
    private String symptomes;
    private String diagnostic;
    private String observations;
    private Double temperature;
    private Double tensionSystolique;
    private Double tensionDiastolique;
    private Double poids;
    private Double taille;
    private List<PrescriptionLigneRequest> prescriptions;
}
