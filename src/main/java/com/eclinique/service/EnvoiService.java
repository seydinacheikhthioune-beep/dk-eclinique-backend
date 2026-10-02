package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.dto.EnvoiRequest;
import com.eclinique.dto.EnvoiResultat;
import com.eclinique.dto.FactureOrganismeResponse;
import com.eclinique.exception.BusinessException;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.Facture;
import com.eclinique.model.FactureOrganisme;
import com.eclinique.model.Patient;
import com.eclinique.model.StatutFacture;
import com.eclinique.repository.FactureOrganismeRepository;
import com.eclinique.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Envoie les factures aux organismes et aux patients, et l'export comptable au comptable. */
@Service
@RequiredArgsConstructor
@Transactional
public class EnvoiService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PDF = "application/pdf";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final EmailService emailService;
    private final PdfService pdfService;
    private final ExportComptableService exportService;
    private final FactureOrganismeService factureOrganismeService;
    private final FactureOrganismeRepository factureOrganismeRepository;
    private final FactureRepository factureRepository;

    @Audite(action = TypeActionAudit.AUTRE, entite = "EnvoiFactureOrganisme")
    public FactureOrganismeResponse envoyerFactureOrganisme(Long id, EnvoiRequest req) {
        FactureOrganisme facture = factureOrganismeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture organisme introuvable : " + id));
        List<String> destinataires = destinataires(req, facture.getOrganisme().getEmail(),
                "L'organisme " + facture.getOrganisme().getNom() + " n'a pas d'adresse e-mail");
        envoyerFactureOrganisme(facture, destinataires, req == null ? null : req.getMessage());
        return factureOrganismeService.findById(id);
    }

    /** Envoie à leur organisme toutes les factures en attente pas encore envoyées. */
    @Audite(action = TypeActionAudit.AUTRE, entite = "EnvoiFacturesOrganismes")
    public List<EnvoiResultat> envoyerFacturesOrganismesEnAttente() {
        List<EnvoiResultat> resultats = new ArrayList<>();
        for (FactureOrganisme f : factureOrganismeRepository.findByStatutAndDateEnvoiIsNullOrderByDateEmissionAsc(StatutFacture.EN_ATTENTE)) {
            String email = f.getOrganisme().getEmail();
            if (email == null || email.isBlank()) {
                resultats.add(new EnvoiResultat(f.getId(), f.getNumero(), f.getOrganisme().getNom(), null, false,
                        "Pas d'adresse e-mail pour cet organisme"));
                continue;
            }
            try {
                List<String> destinataires = emailService.separer(email);
                envoyerFactureOrganisme(f, destinataires, null);
                resultats.add(new EnvoiResultat(f.getId(), f.getNumero(), f.getOrganisme().getNom(),
                        String.join(", ", destinataires), true, "Envoyée"));
            } catch (BusinessException e) {
                resultats.add(new EnvoiResultat(f.getId(), f.getNumero(), f.getOrganisme().getNom(), email, false, e.getMessage()));
            }
        }
        return resultats;
    }

    @Audite(action = TypeActionAudit.AUTRE, entite = "EnvoiFacture")
    public Facture envoyerFacturePatient(Long id, EnvoiRequest req) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + id));
        Patient p = facture.getPatient();
        List<String> destinataires = destinataires(req, p.getEmail(),
                p.getPrenom() + " " + p.getNom() + " n'a pas d'adresse e-mail");
        String texte = "Bonjour " + p.getPrenom() + " " + p.getNom() + ",\n\n"
                + "Veuillez trouver ci-joint votre facture " + facture.getNumeroFacture() + " du "
                + facture.getDateFacture().format(FMT) + ".\n"
                + "Montant total : " + montant(facture.getMontantTotal()) + " FCFA\n"
                + (facture.partOrganismeEffective() > 0
                ? "Pris en charge par " + facture.getOrganisme().getNom() + " : " + montant(facture.partOrganismeEffective()) + " FCFA\n"
                + "Reste à votre charge : " + montant(facture.partPatientEffective()) + " FCFA\n" : "")
                + messageLibre(req) + signature();
        emailService.envoyer(destinataires, "Votre facture " + facture.getNumeroFacture() + " — SEYNI SY MEDICAL", texte,
                List.of(new EmailService.PieceJointe("facture_" + facture.getNumeroFacture() + ".pdf",
                        pdfService.genererRecuFacture(facture), PDF)));
        facture.setDateEnvoi(LocalDateTime.now());
        facture.setEnvoyeA(String.join(", ", destinataires));
        return factureRepository.save(facture);
    }

    @Audite(action = TypeActionAudit.AUTRE, entite = "EnvoiExportComptable")
    public EnvoiResultat envoyerExportComptable(EnvoiRequest req) {
        if (req == null || req.getDebut() == null || req.getFin() == null) {
            throw new BusinessException("La période de l'export est obligatoire");
        }
        List<String> destinataires = emailService.separer(req.getDestinataires());
        if (destinataires.isEmpty()) destinataires = emailService.destinatairesComptables();
        if (destinataires.isEmpty()) {
            throw new BusinessException("Renseignez l'adresse du comptable (ou COMPTABLE_EMAIL sur le serveur)");
        }
        LocalDate debut = req.getDebut(), fin = req.getFin();
        String periode = debut.format(FMT) + " au " + fin.format(FMT);
        String texte = "Bonjour,\n\nVeuillez trouver ci-joint l'export comptable de SEYNI SY MEDICAL pour la période du "
                + periode + " (synthèse, factures patients, factures assurances / IPM, encaissements et paiements des employés).\n"
                + messageLibre(req) + signature();
        emailService.envoyer(destinataires, "Export comptable du " + periode + " — SEYNI SY MEDICAL", texte,
                List.of(new EmailService.PieceJointe(exportService.nomFichier(debut, fin), exportService.exporter(debut, fin), XLSX)));
        return new EnvoiResultat(null, exportService.nomFichier(debut, fin), null, String.join(", ", destinataires), true,
                "Export envoyé");
    }

    private void envoyerFactureOrganisme(FactureOrganisme facture, List<String> destinataires, String message) {
        FactureOrganismeResponse detail = factureOrganismeService.findById(facture.getId());
        String texte = "Bonjour,\n\nVeuillez trouver ci-joint notre facture " + facture.getNumero()
                + " relative aux prises en charge de vos assurés pour la période du "
                + facture.getPeriodeDebut().format(FMT) + " au " + facture.getPeriodeFin().format(FMT) + ".\n"
                + "Nombre de prises en charge : " + detail.nombreLignes() + "\n"
                + "Montant à régler : " + montant(facture.getMontantTotal()) + " FCFA\n"
                + (message == null || message.isBlank() ? "" : "\n" + message.trim() + "\n")
                + signature();
        emailService.envoyer(destinataires, "Facture " + facture.getNumero() + " — SEYNI SY MEDICAL", texte,
                List.of(new EmailService.PieceJointe("facture_" + facture.getNumero() + ".pdf",
                        pdfService.genererFactureOrganisme(detail), PDF)));
        facture.setDateEnvoi(LocalDateTime.now());
        facture.setEnvoyeA(String.join(", ", destinataires));
        factureOrganismeRepository.save(facture);
    }

    /** Adresses saisies, sinon celle du concerné. */
    private List<String> destinataires(EnvoiRequest req, String parDefaut, String siAbsent) {
        List<String> liste = emailService.separer(req == null ? null : req.getDestinataires());
        if (liste.isEmpty()) liste = emailService.separer(parDefaut);
        if (liste.isEmpty()) throw new BusinessException(siAbsent + " : saisissez un destinataire");
        return liste;
    }

    private String messageLibre(EnvoiRequest req) {
        return req == null || req.getMessage() == null || req.getMessage().isBlank() ? "" : "\n" + req.getMessage().trim() + "\n";
    }

    private String signature() {
        return "\nCordialement,\nSEYNI SY MEDICAL\nDarou Khoudoss route de Mboro\nTél : 77 519 35 11 / 76 353 48 42\n";
    }

    private String montant(Double m) {
        return m == null ? "0" : String.format(java.util.Locale.ROOT, "%,.0f", m).replace(',', ' ');
    }
}
