package com.eclinique.controller;

import com.eclinique.dto.EnvoiRequest;
import com.eclinique.dto.EnvoiResultat;
import com.eclinique.dto.FactureOrganismeResponse;
import com.eclinique.model.Facture;
import com.eclinique.service.EmailService;
import com.eclinique.service.EnvoiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Envoi par e-mail des factures aux organismes / patients et de l'export comptable. */
@RestController
@RequestMapping("/envois")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
public class EnvoiController {
    private final EnvoiService envoiService;
    private final EmailService emailService;

    @GetMapping("/parametres")
    public Map<String, Object> parametres() {
        return Map.of("mailConfigure", emailService.estConfigure(),
                "emailsComptable", String.join(", ", emailService.destinatairesComptables()));
    }

    @PostMapping("/factures-organismes/{id}")
    public FactureOrganismeResponse factureOrganisme(@PathVariable Long id, @RequestBody(required = false) EnvoiRequest req) {
        return envoiService.envoyerFactureOrganisme(id, req);
    }

    @PostMapping("/factures-organismes/en-attente")
    public List<EnvoiResultat> facturesOrganismesEnAttente() {
        return envoiService.envoyerFacturesOrganismesEnAttente();
    }

    @PostMapping("/factures/{id}")
    public Facture facturePatient(@PathVariable Long id, @RequestBody(required = false) EnvoiRequest req) {
        return envoiService.envoyerFacturePatient(id, req);
    }

    @PostMapping("/export-comptable")
    @PreAuthorize("hasRole('ADMIN')")
    public EnvoiResultat exportComptable(@RequestBody EnvoiRequest req) {
        return envoiService.envoyerExportComptable(req);
    }
}
