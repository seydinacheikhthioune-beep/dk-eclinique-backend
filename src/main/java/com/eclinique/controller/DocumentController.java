package com.eclinique.controller;

import com.eclinique.model.Consultation;
import com.eclinique.model.Facture;
import com.eclinique.model.Patient;
import com.eclinique.service.ConsultationService;
import com.eclinique.service.FactureService;
import com.eclinique.service.PatientService;
import com.eclinique.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints d'impression : reçus de facture et rapports médicaux détaillés (PDF).
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DocumentController {

    private final PdfService pdfService;
    private final FactureService factureService;
    private final PatientService patientService;
    private final ConsultationService consultationService;

    @GetMapping("/factures/{id}/recu")
    public ResponseEntity<byte[]> recuFacture(@PathVariable Long id) {
        Facture facture = factureService.findById(id);
        byte[] pdf = pdfService.genererRecuFacture(facture);
        return construireReponsePdf(pdf, "recu_" + facture.getNumeroFacture() + ".pdf");
    }

    @GetMapping("/patients/{id}/rapport-medical")
    public ResponseEntity<byte[]> rapportMedical(@PathVariable Long id) {
        Patient patient = patientService.findById(id);
        var consultations = consultationService.findByPatient(id);
        byte[] pdf = pdfService.genererRapportMedical(patient, consultations);
        return construireReponsePdf(pdf, "rapport_medical_" + patient.getNumeroDossier() + ".pdf");
    }

    private ResponseEntity<byte[]> construireReponsePdf(byte[] contenu, String nomFichier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(nomFichier).build());
        return ResponseEntity.ok().headers(headers).body(contenu);
    }
}
