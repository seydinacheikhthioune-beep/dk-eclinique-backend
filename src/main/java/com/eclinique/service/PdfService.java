package com.eclinique.service;

import com.eclinique.model.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère les documents PDF : reçus de facture et rapports médicaux détaillés.
 */
@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITRE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(25, 60, 110));
    private static final Font SOUS_TITRE_FONT = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(25, 60, 110));
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font GRAS_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font PETIT_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);

    // ==================== REÇU DE FACTURE ====================

    public byte[] genererRecuFacture(Facture facture) {
        if (facture == null) {
            throw new IllegalArgumentException("Facture introuvable pour la génération du reçu.");
        }

        try {
            Document document = new Document(PageSize.A5, 30, 30, 30, 30);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            enTete(document, "REÇU DE PAIEMENT");

            Patient p = facture.getPatient();
            PdfPTable infos = new PdfPTable(2);
            infos.setWidthPercentage(100);
            infos.setSpacingBefore(10);
            ajouterLigneInfo(infos, "N° Facture :", safeText(facture.getNumeroFacture()));
            ajouterLigneInfo(infos, "Date :", facture.getDateFacture() != null ? facture.getDateFacture().format(DATE_HEURE_FMT) : "-");
            ajouterLigneInfo(infos, "Patient :", p != null ? (p.getPrenom() + " " + p.getNom()).trim() : "-");
            ajouterLigneInfo(infos, "N° Dossier :", p != null && p.getNumeroDossier() != null ? p.getNumeroDossier() : "-");
            ajouterLigneInfo(infos, "Statut :", traduireStatutFacture(facture.getStatut()));
            if (facture.getModePaiement() != null) {
                ajouterLigneInfo(infos, "Mode de paiement :", traduireModePaiement(facture.getModePaiement()));
            }
            document.add(infos);

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{4, 1, 1.5f, 1.5f});
            table.setWidthPercentage(100);
            enteteCellule(table, "Désignation");
            enteteCellule(table, "Qté");
            enteteCellule(table, "P.U (FCFA)");
            enteteCellule(table, "Montant (FCFA)");

            List<LigneFacture> lignes = facture.getLignes() == null ? List.of() : facture.getLignes();
            for (LigneFacture ligne : lignes) {
                celluleTexte(table, safeText(ligne.getDesignation()));
                celluleTexteCentre(table, String.valueOf(ligne.getQuantite()));
                celluleTexteDroite(table, formatMontant(ligne.getPrixUnitaire()));
                celluleTexteDroite(table, formatMontant(ligne.getMontant()));
            }
            document.add(table);

            document.add(new Paragraph(" "));

            double sousTotal = lignes.stream().mapToDouble(l -> l.getMontant() != null ? l.getMontant() : 0).sum();
            double tva = sousTotal * 0.18;
            double total = sousTotal + tva;
            if (facture.getRemise() != null && facture.getRemise() > 0) {
                total = Math.max(total - facture.getRemise(), 0);
            }

            PdfPTable totaux = new PdfPTable(2);
            totaux.setWidthPercentage(50);
            totaux.setHorizontalAlignment(Element.ALIGN_RIGHT);
            ajouterLigneInfo(totaux, "Sous-total :", formatMontant(sousTotal) + " FCFA");
            ajouterLigneInfo(totaux, "TVA (18%) :", formatMontant(tva) + " FCFA");
            if (facture.getRemise() != null && facture.getRemise() > 0) {
                ajouterLigneInfo(totaux, "Remise :", formatMontant(facture.getRemise()) + " FCFA");
            }
            PdfPCell libTotal = new PdfPCell(new Phrase("TOTAL À PAYER :", GRAS_FONT));
            libTotal.setBorder(Rectangle.TOP);
            PdfPCell valTotal = new PdfPCell(new Phrase(formatMontant(total) + " FCFA", GRAS_FONT));
            valTotal.setBorder(Rectangle.TOP);
            valTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totaux.addCell(libTotal);
            totaux.addCell(valTotal);
            document.add(totaux);

            piedDePage(document, "Merci de votre confiance. Ce reçu fait foi de paiement.");

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du reçu PDF", e);
        }
    }

    // ==================== RAPPORT MÉDICAL DÉTAILLÉ ====================

    public byte[] genererRapportMedical(Patient patient, List<Consultation> consultations) {
        try {
            Document document = new Document(PageSize.A4, 40, 40, 50, 40);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            enTete(document, "RAPPORT MÉDICAL DÉTAILLÉ");

            // Informations patient
            document.add(new Paragraph("Informations du patient", SOUS_TITRE_FONT));
            PdfPTable infosPatient = new PdfPTable(2);
            infosPatient.setWidthPercentage(100);
            infosPatient.setSpacingBefore(8);
            infosPatient.setSpacingAfter(15);
            ajouterLigneInfo(infosPatient, "Nom complet :", patient.getPrenom() + " " + patient.getNom());
            ajouterLigneInfo(infosPatient, "N° Dossier :", patient.getNumeroDossier() == null ? "-" : patient.getNumeroDossier());
            ajouterLigneInfo(infosPatient, "Date de naissance :",
                    patient.getDateNaissance() != null ? patient.getDateNaissance().format(DATE_FMT) : "-");
            ajouterLigneInfo(infosPatient, "Sexe :", patient.getSexe() != null ? patient.getSexe().name() : "-");
            ajouterLigneInfo(infosPatient, "Groupe sanguin :", patient.getGroupeSanguin() != null ? patient.getGroupeSanguin() : "-");
            ajouterLigneInfo(infosPatient, "Allergies :", videSiNull(patient.getAllergies()));
            ajouterLigneInfo(infosPatient, "Antécédents médicaux :", videSiNull(patient.getAntecedentsMedicaux()));
            document.add(infosPatient);

            document.add(new Paragraph("Historique des consultations", SOUS_TITRE_FONT));
            document.add(new Paragraph(" "));

            if (consultations == null || consultations.isEmpty()) {
                document.add(new Paragraph("Aucune consultation enregistrée.", NORMAL_FONT));
            }

            for (Consultation c : consultations) {
                PdfPTable enteteConsult = new PdfPTable(1);
                enteteConsult.setWidthPercentage(100);
                PdfPCell titreConsult = new PdfPCell(new Phrase(
                        "Consultation du " + c.getDateConsultation().format(DATE_HEURE_FMT) +
                                (c.getMedecin() != null ? "  —  Dr. " + c.getMedecin().getPrenom() + " " + c.getMedecin().getNom() : ""),
                        GRAS_FONT));
                titreConsult.setBackgroundColor(new BaseColor(230, 238, 250));
                titreConsult.setPadding(6);
                enteteConsult.addCell(titreConsult);
                document.add(enteteConsult);

                PdfPTable details = new PdfPTable(2);
                details.setWidthPercentage(100);
                details.setSpacingBefore(4);
                ajouterLigneInfo(details, "Motif :", videSiNull(c.getMotif()));
                ajouterLigneInfo(details, "Symptômes :", videSiNull(c.getSymptomes()));
                ajouterLigneInfo(details, "Diagnostic :", videSiNull(c.getDiagnostic()));
                ajouterLigneInfo(details, "Observations :", videSiNull(c.getObservations()));

                StringBuilder constantes = new StringBuilder();
                if (c.getTemperature() != null) constantes.append("Temp: ").append(c.getTemperature()).append("°C  ");
                if (c.getTensionArterielle_systolique() != null && c.getTensionArterielle_diastolique() != null) {
                    constantes.append("TA: ").append(c.getTensionArterielle_systolique().intValue())
                            .append("/").append(c.getTensionArterielle_diastolique().intValue()).append(" mmHg  ");
                }
                if (c.getPoids() != null) constantes.append("Poids: ").append(c.getPoids()).append("kg  ");
                if (c.getTaille() != null) constantes.append("Taille: ").append(c.getTaille()).append("cm");
                if (constantes.length() > 0) {
                    ajouterLigneInfo(details, "Constantes :", constantes.toString());
                }
                document.add(details);

                if (c.getPrescriptions() != null && !c.getPrescriptions().isEmpty()) {
                    document.add(new Paragraph("Prescriptions :", GRAS_FONT));
                    PdfPTable presc = new PdfPTable(new float[]{3, 1, 2, 1.5f});
                    presc.setWidthPercentage(100);
                    presc.setSpacingBefore(4);
                    enteteCellule(presc, "Médicament");
                    enteteCellule(presc, "Qté");
                    enteteCellule(presc, "Posologie");
                    enteteCellule(presc, "Durée");
                    for (PrescriptionLigne pl : c.getPrescriptions()) {
                        celluleTexte(presc, pl.getMedicament() != null ? pl.getMedicament().getNom() : "-");
                        celluleTexteCentre(presc, String.valueOf(pl.getQuantite()));
                        celluleTexte(presc, videSiNull(pl.getPosologie()));
                        celluleTexte(presc, videSiNull(pl.getDureeTraitement()));
                    }
                    document.add(presc);
                }

                document.add(new Paragraph(" "));
            }

            piedDePage(document, "Document confidentiel — usage médical exclusivement.");

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du rapport médical PDF", e);
        }
    }

    // ==================== OUTILS COMMUNS ====================

    private void enTete(Document document, String titre) throws DocumentException {
        Paragraph clinique = new Paragraph("E-CLINIQUE", TITRE_FONT);
        clinique.setAlignment(Element.ALIGN_CENTER);
        document.add(clinique);

        Paragraph sousTitre = new Paragraph(titre, SOUS_TITRE_FONT);
        sousTitre.setAlignment(Element.ALIGN_CENTER);
        sousTitre.setSpacingAfter(10);
        document.add(sousTitre);

        LineSeparator ligne = new LineSeparator();
        ligne.setLineColor(new BaseColor(25, 60, 110));
        document.add(new Chunk(ligne));
        document.add(new Paragraph(" "));
    }

    private void piedDePage(Document document, String message) throws DocumentException {
        document.add(new Paragraph(" "));
        Paragraph pied = new Paragraph(message, PETIT_FONT);
        pied.setAlignment(Element.ALIGN_CENTER);
        pied.setSpacingBefore(20);
        document.add(pied);
    }

    private void ajouterLigneInfo(PdfPTable table, String libelle, String valeur) {
        PdfPCell c1 = new PdfPCell(new Phrase(libelle, GRAS_FONT));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPaddingBottom(4);
        PdfPCell c2 = new PdfPCell(new Phrase(valeur == null ? "-" : valeur, NORMAL_FONT));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPaddingBottom(4);
        table.addCell(c1);
        table.addCell(c2);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void enteteCellule(PdfPTable table, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, GRAS_FONT));
        cell.setBackgroundColor(new BaseColor(25, 60, 110));
        cell.getPhrase().getFont().setColor(BaseColor.WHITE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void celluleTexte(PdfPTable table, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte == null ? "-" : texte, NORMAL_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void celluleTexteCentre(PdfPTable table, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, NORMAL_FONT));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void celluleTexteDroite(PdfPTable table, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, NORMAL_FONT));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private String videSiNull(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String formatMontant(Double m) {
        if (m == null) return "0";
        return String.format("%,.0f", m).replace(",", " ");
    }

    private String traduireStatutFacture(StatutFacture s) {
        if (s == null) return "-";
        return switch (s) {
            case EN_ATTENTE -> "En attente";
            case PAYEE -> "Payée";
            case ANNULEE -> "Annulée";
        };
    }

    private String traduireModePaiement(ModePaiement m) {
        if (m == null) return "-";
        return switch (m) {
            case ESPECES -> "Espèces";
            case CARTE_BANCAIRE -> "Carte bancaire";
            case MOBILE_MONEY -> "Mobile Money";
            case ASSURANCE -> "Assurance";
            case VIREMENT -> "Virement";
        };
    }
}
