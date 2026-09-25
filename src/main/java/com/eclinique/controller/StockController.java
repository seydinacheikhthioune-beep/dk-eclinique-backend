package com.eclinique.controller;

import com.eclinique.dto.MouvementStockRequest;
import com.eclinique.model.MouvementStock;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StockController {

    private final StockService stockService;

    @GetMapping("/mouvements")
    public List<MouvementStock> historique(@RequestParam(required = false) Long medicamentId) {
        return stockService.historiqueMouvements(medicamentId);
    }

    @PostMapping("/mouvements")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    public ResponseEntity<MouvementStock> enregistrer(@Valid @RequestBody MouvementStockRequest request,
                                                        @AuthenticationPrincipal UtilisateurPrincipal principal) {
        MouvementStock mouvement = stockService.enregistrerMouvement(
                request.getMedicamentId(), request.getType(), request.getQuantite(),
                request.getMotif(), principal.getId());
        return ResponseEntity.ok(mouvement);
    }
}
