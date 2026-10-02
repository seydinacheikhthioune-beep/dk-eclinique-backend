package com.eclinique.service;

import com.eclinique.model.*;
import com.eclinique.repository.EncaissementRepository;
import com.eclinique.repository.FactureOrganismeRepository;
import com.eclinique.repository.FactureRepository;
import com.eclinique.repository.PaiementEmployeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Export comptable Excel d'une période : synthèse, factures patients, factures organismes,
 * encaissements d'accueil et paiements des employés. Les montants sont des nombres (pas du texte)
 * pour que le comptable puisse les additionner directement.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportComptableService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FactureRepository factureRepository;
    private final FactureOrganismeRepository factureOrganismeRepository;
    private final EncaissementRepository encaissementRepository;
    private final PaiementEmployeRepository paiementRepository;

    public String nomFichier(LocalDate debut, LocalDate fin) {
        return "export-comptable_" + debut + "_" + fin + ".xlsx";
    }

    public byte[] exporter(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }
        LocalDateTime de = debut.atStartOfDay();
        LocalDateTime a = fin.plusDays(1).atStartOfDay();
        List<Facture> factures = factureRepository.findByDateFactureGreaterThanEqualAndDateFactureLessThanOrderByDateFactureAsc(de, a);
        List<FactureOrganisme> facturesOrg = factureOrganismeRepository
                .findByDateEmissionGreaterThanEqualAndDateEmissionLessThanOrderByDateEmissionAsc(de, a);
        List<Encaissement> encaissements = encaissementRepository
                .findByDateEncaissementGreaterThanEqualAndDateEncaissementLessThan(de, a).stream()
                .sorted(java.util.Comparator.comparing(Encaissement::getDateEncaissement)).toList();
        List<PaiementEmploye> paiements = paiementRepository
                .findByDatePaiementGreaterThanEqualAndDatePaiementLessThan(de, a).stream()
                .sorted(java.util.Comparator.comparing(PaiementEmploye::getDatePaiement)).toList();
        Map<Long, String> numerosOrg = factureOrganismeRepository.findAll().stream()
                .collect(Collectors.toMap(FactureOrganisme::getId, FactureOrganisme::getNumero));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles st = new Styles(wb);
            synthese(wb, st, debut, fin, factures, facturesOrg, encaissements, paiements);
            feuilleFactures(wb, st, factures, numerosOrg);
            feuilleFacturesOrganismes(wb, st, facturesOrg);
            feuilleEncaissements(wb, st, encaissements);
            feuillePaiements(wb, st, paiements);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la génération de l'export comptable", e);
        }
    }

    // ---------- Feuilles ----------

    private void synthese(Workbook wb, Styles st, LocalDate debut, LocalDate fin, List<Facture> factures,
                          List<FactureOrganisme> facturesOrg, List<Encaissement> encaissements,
                          List<PaiementEmploye> paiements) {
        Sheet s = wb.createSheet("Synthèse");
        List<Facture> valides = factures.stream().filter(f -> f.getStatut() != StatutFacture.ANNULEE).toList();
        List<FactureOrganisme> orgValides = facturesOrg.stream().filter(f -> f.getStatut() != StatutFacture.ANNULEE).toList();
        double encPatients = encaissements.stream().mapToDouble(Encaissement::partPatientEffective).sum();
        double encOrganismes = encaissements.stream().mapToDouble(Encaissement::partOrganismeEffective).sum();
        double facTotal = valides.stream().mapToDouble(f -> valeur(f.getMontantTotal())).sum();
        double facPatients = valides.stream().mapToDouble(Facture::partPatientEffective).sum();
        double facOrganismes = valides.stream().mapToDouble(Facture::partOrganismeEffective).sum();
        double facPayees = valides.stream().filter(f -> f.getStatut() == StatutFacture.PAYEE)
                .mapToDouble(Facture::partPatientEffective).sum();
        double depenses = paiements.stream().mapToDouble(p -> valeur(p.getMontant())).sum();
        double recettes = encPatients + encOrganismes + facTotal;

        Row titre = s.createRow(0);
        cellule(titre, 0, "Export comptable — SEYNI SY MEDICAL", st.titre);
        s.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        cellule(s.createRow(1), 0, "Période du " + debut.format(FMT) + " au " + fin.format(FMT), null);
        cellule(s.createRow(2), 0, "Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), null);

        Object[][] lignes = {
                {"RECETTES", null, null},
                {"Encaissements à l'accueil — part patients", encaissements.size(), encPatients},
                {"Encaissements à l'accueil — part assurances / IPM", null, encOrganismes},
                {"Factures patients émises (hors annulées)", valides.size(), facTotal},
                {"   dont part patients", null, facPatients},
                {"   dont part assurances / IPM (tiers-payant)", null, facOrganismes},
                {"   dont part patients déjà réglée", null, facPayees},
                {"Total recettes", null, recettes},
                {null, null, null},
                {"DÉPENSES", null, null},
                {"Paiements des employés", paiements.size(), depenses},
                {null, null, null},
                {"SOLDE (recettes − dépenses)", null, recettes - depenses},
                {null, null, null},
                {"ASSURANCES / IPM", null, null},
                {"Factures organismes émises (hors annulées)", orgValides.size(),
                        orgValides.stream().mapToDouble(f -> valeur(f.getMontantTotal())).sum()},
                {"   dont réglées", null, orgValides.stream().filter(f -> f.getStatut() == StatutFacture.PAYEE)
                        .mapToDouble(f -> valeur(f.getMontantTotal())).sum()},
                {"   dont en attente de règlement", null, orgValides.stream().filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE)
                        .mapToDouble(f -> valeur(f.getMontantTotal())).sum()},
        };
        Row entete = s.createRow(4);
        cellule(entete, 0, "Poste", st.entete);
        cellule(entete, 1, "Nombre", st.entete);
        cellule(entete, 2, "Montant (FCFA)", st.entete);
        int r = 5;
        for (Object[] l : lignes) {
            Row row = s.createRow(r++);
            if (l[0] == null) continue;
            boolean section = l[1] == null && l[2] == null;
            boolean total = ((String) l[0]).startsWith("Total") || ((String) l[0]).startsWith("SOLDE");
            cellule(row, 0, (String) l[0], section ? st.section : total ? st.gras : null);
            if (l[1] != null) row.createCell(1).setCellValue(((Number) l[1]).doubleValue());
            if (l[2] != null) {
                Cell c = row.createCell(2);
                c.setCellValue((Double) l[2]);
                c.setCellStyle(total ? st.montantGras : st.montant);
            }
        }
        s.setColumnWidth(0, 52 * 256);
        s.setColumnWidth(1, 10 * 256);
        s.setColumnWidth(2, 18 * 256);
    }

    private void feuilleFactures(Workbook wb, Styles st, List<Facture> factures, Map<Long, String> numerosOrg) {
        Sheet s = feuille(wb, st, "Factures patients", "N° facture", "Date", "Patient", "N° dossier", "Nature", "Statut",
                "Mode de paiement", "Date paiement", "Remise", "Montant total", "Organisme", "Matricule", "Taux %",
                "Part organisme", "Part patient", "Facture organisme", "Envoyée le");
        int r = 1;
        for (Facture f : factures) {
            Patient p = f.getPatient();
            Row row = s.createRow(r++);
            int c = 0;
            texte(row, c++, f.getNumeroFacture());
            date(row, c++, f.getDateFacture(), st);
            texte(row, c++, p == null ? null : p.getPrenom() + " " + p.getNom());
            texte(row, c++, p == null ? null : p.getNumeroDossier());
            texte(row, c++, f.getDateAdmission() != null ? "Hospitalisation" : f.getConsultation() != null ? "Consultation" : "Soins");
            texte(row, c++, statut(f.getStatut()));
            texte(row, c++, mode(f.getModePaiement()));
            date(row, c++, f.getDatePaiement(), st);
            montant(row, c++, f.getRemise(), st);
            montant(row, c++, f.getMontantTotal(), st);
            texte(row, c++, f.getOrganisme() == null ? null : f.getOrganisme().getNom());
            texte(row, c++, f.getMatriculeAssure());
            montant(row, c++, f.getTauxPriseEnCharge(), st);
            montant(row, c++, f.partOrganismeEffective(), st);
            montant(row, c++, f.partPatientEffective(), st);
            texte(row, c++, f.getFactureOrganismeId() == null ? null : numerosOrg.get(f.getFactureOrganismeId()));
            date(row, c, f.getDateEnvoi(), st);
        }
        totaux(s, st, r, 8, 9, 13, 14);
        finaliser(s, 17);
    }

    private void feuilleFacturesOrganismes(Workbook wb, Styles st, List<FactureOrganisme> factures) {
        Sheet s = feuille(wb, st, "Factures organismes", "N° facture", "Date d'émission", "Organisme", "Type", "NINEA",
                "Période du", "Période au", "Montant", "Statut", "Mode de paiement", "Date paiement", "Envoyée le", "Envoyée à");
        int r = 1;
        for (FactureOrganisme f : factures) {
            Row row = s.createRow(r++);
            int c = 0;
            texte(row, c++, f.getNumero());
            date(row, c++, f.getDateEmission(), st);
            texte(row, c++, f.getOrganisme().getNom());
            texte(row, c++, f.getOrganisme().getType() == TypeOrganisme.IPM ? "IPM" : "Assurance");
            texte(row, c++, f.getOrganisme().getNinea());
            date(row, c++, f.getPeriodeDebut().atStartOfDay(), st);
            date(row, c++, f.getPeriodeFin().atStartOfDay(), st);
            montant(row, c++, f.getMontantTotal(), st);
            texte(row, c++, statut(f.getStatut()));
            texte(row, c++, mode(f.getModePaiement()));
            date(row, c++, f.getDatePaiement(), st);
            date(row, c++, f.getDateEnvoi(), st);
            texte(row, c, f.getEnvoyeA());
        }
        totaux(s, st, r, 7);
        finaliser(s, 13);
    }

    private void feuilleEncaissements(Workbook wb, Styles st, List<Encaissement> encaissements) {
        Sheet s = feuille(wb, st, "Encaissements", "Date", "Type", "Patient", "N° dossier", "Médecin", "Organisme",
                "Matricule", "Montant", "Part patient", "Part organisme", "Enregistré par");
        int r = 1;
        for (Encaissement e : encaissements) {
            Row row = s.createRow(r++);
            int c = 0;
            date(row, c++, e.getDateEncaissement(), st);
            texte(row, c++, e.getType() == TypeEncaissement.RENDEZVOUS ? "Rendez-vous"
                    : "Consultation" + ("SPECIALISEE".equals(e.getTypeConsultation()) ? " spécialisée" : ""));
            texte(row, c++, e.getPatientNom());
            texte(row, c++, e.getNumeroDossier());
            texte(row, c++, e.getMedecinNom());
            texte(row, c++, e.getOrganismeNom() == null ? "Comptant" : e.getOrganismeNom());
            texte(row, c++, e.getMatriculeAssure());
            montant(row, c++, e.getMontant(), st);
            montant(row, c++, e.partPatientEffective(), st);
            montant(row, c++, e.partOrganismeEffective(), st);
            texte(row, c, e.getEnregistreParNom());
        }
        totaux(s, st, r, 7, 8, 9);
        finaliser(s, 11);
    }

    private void feuillePaiements(Workbook wb, Styles st, List<PaiementEmploye> paiements) {
        Sheet s = feuille(wb, st, "Paiements employés", "Date", "Employé", "Période", "Motif", "Montant");
        int r = 1;
        for (PaiementEmploye p : paiements) {
            Row row = s.createRow(r++);
            date(row, 0, p.getDatePaiement(), st);
            texte(row, 1, p.getEmploye() == null ? null : p.getEmploye().getPrenom() + " " + p.getEmploye().getNom());
            texte(row, 2, p.getPeriode());
            texte(row, 3, p.getMotif());
            montant(row, 4, p.getMontant(), st);
        }
        totaux(s, st, r, 4);
        finaliser(s, 5);
    }

    // ---------- Outils ----------

    private static final class Styles {
        final CellStyle entete, titre, section, gras, montant, montantGras, date;

        Styles(Workbook wb) {
            DataFormat df = wb.createDataFormat();
            Font fGras = wb.createFont();
            fGras.setBold(true);
            Font fBlanc = wb.createFont();
            fBlanc.setBold(true);
            fBlanc.setColor(IndexedColors.WHITE.getIndex());
            Font fTitre = wb.createFont();
            fTitre.setBold(true);
            fTitre.setFontHeightInPoints((short) 14);

            entete = wb.createCellStyle();
            entete.setFont(fBlanc);
            entete.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
            entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titre = wb.createCellStyle();
            titre.setFont(fTitre);
            section = wb.createCellStyle();
            section.setFont(fGras);
            section.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            gras = wb.createCellStyle();
            gras.setFont(fGras);
            montant = wb.createCellStyle();
            montant.setDataFormat(df.getFormat("#,##0"));
            montantGras = wb.createCellStyle();
            montantGras.setDataFormat(df.getFormat("#,##0"));
            montantGras.setFont(fGras);
            date = wb.createCellStyle();
            date.setDataFormat(df.getFormat("dd/mm/yyyy"));
        }
    }

    private Sheet feuille(Workbook wb, Styles st, String nom, String... colonnes) {
        Sheet s = wb.createSheet(nom);
        Row entete = s.createRow(0);
        for (int i = 0; i < colonnes.length; i++) cellule(entete, i, colonnes[i], st.entete);
        s.createFreezePane(0, 1);
        return s;
    }

    /** Ligne de totaux (formules SUM) sous les données pour les colonnes données. */
    private void totaux(Sheet s, Styles st, int ligne, int... colonnes) {
        if (ligne <= 1) return;
        Row row = s.createRow(ligne);
        cellule(row, 0, "TOTAL", st.gras);
        for (int c : colonnes) {
            String col = org.apache.poi.ss.util.CellReference.convertNumToColString(c);
            Cell cell = row.createCell(c);
            cell.setCellFormula("SUM(" + col + "2:" + col + ligne + ")");
            cell.setCellStyle(st.montantGras);
        }
    }

    private void finaliser(Sheet s, int nbColonnes) {
        if (s.getLastRowNum() > 0) s.setAutoFilter(new CellRangeAddress(0, s.getLastRowNum() - 1, 0, nbColonnes - 1));
        // Largeur calculée à la main : autoSizeColumn dépend des polices AWT, absentes des images Docker légères
        int[] largeurs = new int[nbColonnes];
        DataFormatter formatter = new DataFormatter();
        for (Row row : s) {
            for (int i = 0; i < nbColonnes; i++) {
                Cell c = row.getCell(i);
                if (c == null) continue;
                int longueur = c.getCellType() == CellType.FORMULA ? 12 : formatter.formatCellValue(c).length();
                largeurs[i] = Math.max(largeurs[i], longueur);
            }
        }
        for (int i = 0; i < nbColonnes; i++) s.setColumnWidth(i, Math.min(Math.max(largeurs[i] + 3, 10), 45) * 256);
    }

    private void cellule(Row row, int col, String valeur, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(valeur);
        if (style != null) c.setCellStyle(style);
    }

    private void texte(Row row, int col, String valeur) {
        if (valeur != null) row.createCell(col).setCellValue(valeur);
    }

    private void montant(Row row, int col, Double valeur, Styles st) {
        if (valeur == null) return;
        Cell c = row.createCell(col);
        c.setCellValue(valeur);
        c.setCellStyle(st.montant);
    }

    private void date(Row row, int col, LocalDateTime valeur, Styles st) {
        if (valeur == null) return;
        Cell c = row.createCell(col);
        c.setCellValue(valeur);
        c.setCellStyle(st.date);
    }

    private String statut(StatutFacture s) {
        if (s == null) return null;
        return switch (s) {
            case EN_ATTENTE -> "En attente";
            case PAYEE -> "Payée";
            case ANNULEE -> "Annulée";
        };
    }

    private String mode(ModePaiement m) {
        if (m == null) return null;
        return switch (m) {
            case ESPECES -> "Espèces";
            case CARTE_BANCAIRE -> "Carte bancaire";
            case MOBILE_MONEY -> "Mobile money";
            case ASSURANCE -> "Assurance";
            case VIREMENT -> "Virement";
        };
    }

    private double valeur(Double d) { return d == null ? 0 : d; }
}
