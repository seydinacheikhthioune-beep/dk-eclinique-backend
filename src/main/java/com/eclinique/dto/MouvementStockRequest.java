package com.eclinique.dto;

import com.eclinique.model.TypeMouvementStock;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MouvementStockRequest {
    @NotNull
    private Long medicamentId;
    @NotNull
    private TypeMouvementStock type;
    @NotNull
    private Integer quantite;
    private String motif;
}
