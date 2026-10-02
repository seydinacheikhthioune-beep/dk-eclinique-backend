package com.eclinique.controller;

import com.eclinique.dto.BilanPeriodeResponse;
import com.eclinique.dto.ComptabiliteResponse;
import com.eclinique.dto.EncaissementResponse;
import com.eclinique.dto.PaiementEmployeRequest;
import com.eclinique.dto.PaiementEmployeResponse;
import com.eclinique.model.PaiementEmploye;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.ComptabiliteService;
import com.eclinique.service.EncaissementService;
import com.eclinique.service.ExportComptableService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comptabilite")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ComptabiliteController {
    private final ComptabiliteService service;
    private final EncaissementService encaissementService;
    private final ExportComptableService exportService;

    @GetMapping("/resume")
    public ComptabiliteResponse resume() { return service.resume(); }

    @GetMapping("/bilan")
    public List<BilanPeriodeResponse> bilan(@RequestParam(defaultValue = "MOIS") String periode,
                                            @RequestParam(required = false) Integer annee) {
        return service.bilan(periode, annee != null ? annee : java.time.Year.now().getValue());
    }

    /** Export Excel de la période pour le comptable. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(exportService.nomFichier(debut, fin)).build());
        return ResponseEntity.ok().headers(headers).body(exportService.exporter(debut, fin));
    }

    @GetMapping("/encaissements")
    public List<EncaissementResponse> encaissements() { return encaissementService.findAll(); }

    @GetMapping("/paiements")
    public List<PaiementEmployeResponse> paiements() { return service.paiements(); }

    @PostMapping("/paiements")
    public PaiementEmploye payer(@Valid @RequestBody PaiementEmployeRequest request,
                                 @AuthenticationPrincipal UtilisateurPrincipal auteur) {
        return service.payer(request, auteur);
    }
}