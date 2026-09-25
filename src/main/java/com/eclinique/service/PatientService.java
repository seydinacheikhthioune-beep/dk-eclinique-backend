package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.dto.PatientImportResult;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.Patient;
import com.eclinique.model.Sexe;
import com.eclinique.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.Year;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    public List<Patient> findAll() {
        return patientRepository.findAll().stream()
            .sorted(Comparator.comparing(Patient::getDateCreation,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable : " + id));
    }

    public List<Patient> rechercher(String motCle) {
        return patientRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(motCle, motCle).stream()
            .sorted(Comparator.comparing(Patient::getDateCreation,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    @Audite(action = TypeActionAudit.CREATION, entite = "Patient")
    public Patient create(Patient patient) {
        patient.setNumeroDossier(genererNumeroDossier());
        return patientRepository.save(patient);
    }

    public PatientImportResult importer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier d'import est vide");
        }
        String nom = fichier.getOriginalFilename() == null ? "" : fichier.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (nom.endsWith(".xlsx") || nom.endsWith(".xls")) {
                return importerExcel(fichier.getInputStream());
            }
            if (nom.endsWith(".docx")) {
                return importerWord(fichier.getInputStream());
            }
            throw new IllegalArgumentException("Format non supporte. Utilisez un fichier .xlsx, .xls ou .docx");
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier d'import", e);
        }
    }

    private PatientImportResult importerExcel(InputStream contenu) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(contenu)) {
            var sheet = workbook.getSheetAt(0);
            Row entetes = sheet.getRow(sheet.getFirstRowNum());
            if (entetes == null) throw new IllegalArgumentException("Le fichier Excel ne contient aucune ligne");
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> colonnes = lireEntetes(entetes, formatter);
            List<List<String>> lignes = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row ligne = sheet.getRow(i);
                if (ligne == null) continue;
                List<String> valeurs = new ArrayList<>();
                for (int c = 0; c < entetes.getLastCellNum(); c++) {
                    Cell cell = ligne.getCell(c);
                    valeurs.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
                lignes.add(valeurs);
            }
            return importerLignes(colonnes, lignes);
        }
    }

    private PatientImportResult importerWord(InputStream contenu) throws IOException {
        try (XWPFDocument document = new XWPFDocument(contenu)) {
            if (document.getTables().isEmpty()) {
                throw new IllegalArgumentException("Le document Word doit contenir un tableau avec une ligne d'en-tetes");
            }
            XWPFTable table = document.getTables().get(0);
            if (table.getRows().isEmpty()) throw new IllegalArgumentException("Le tableau Word est vide");
            List<String> entetes = table.getRows().get(0).getTableCells().stream()
                    .map(cell -> cell.getText().trim()).toList();
            Map<String, Integer> colonnes = lireEntetes(entetes);
            List<List<String>> lignes = new ArrayList<>();
            for (int i = 1; i < table.getRows().size(); i++) {
                lignes.add(table.getRows().get(i).getTableCells().stream()
                        .map(cell -> cell.getText().trim()).toList());
            }
            return importerLignes(colonnes, lignes);
        }
    }

    private PatientImportResult importerLignes(Map<String, Integer> colonnes, List<List<String>> lignes) {
        verifierColonnesObligatoires(colonnes);
        Set<String> identites = new HashSet<>();
        Set<String> emails = new HashSet<>();
        patientRepository.findAll().forEach(patient -> {
            identites.add(cleIdentite(patient.getNom(), patient.getPrenom()));
            if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
                emails.add(patient.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        });
        int importes = 0;
        int ignores = 0;
        List<String> erreurs = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            try {
                Patient patient = convertirPatient(lignes.get(i), colonnes);
                String identite = cleIdentite(patient.getNom(), patient.getPrenom());
                String email = patient.getEmail() == null ? "" : patient.getEmail().trim().toLowerCase(Locale.ROOT);
                if (identites.contains(identite) || (!email.isBlank() && emails.contains(email))) {
                    ignores++;
                    continue;
                }
                create(patient);
                identites.add(identite);
                if (!email.isBlank()) emails.add(email);
                importes++;
            } catch (RuntimeException e) {
                erreurs.add("Ligne " + (i + 2) + " : " + e.getMessage());
            }
        }
        return new PatientImportResult(importes, ignores, erreurs);
    }

    private Patient convertirPatient(List<String> valeurs, Map<String, Integer> colonnes) {
        Patient patient = new Patient();
        patient.setNom(valeur(valeurs, colonnes, "nom"));
        patient.setPrenom(valeur(valeurs, colonnes, "prenom"));
        if (patient.getNom().isBlank() || patient.getPrenom().isBlank()) {
            throw new IllegalArgumentException("nom et prenom sont obligatoires");
        }
        patient.setDateNaissance(lireDate(valeur(valeurs, colonnes, "datenaissance")));
        String sexe = valeur(valeurs, colonnes, "sexe").toUpperCase(Locale.ROOT);
        if (!sexe.isBlank()) {
            try { patient.setSexe(Sexe.valueOf(sexe)); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("sexe doit etre HOMME ou FEMME"); }
        }
        patient.setAdresse(valeur(valeurs, colonnes, "adresse"));
        patient.setTelephone(valeur(valeurs, colonnes, "telephone"));
        patient.setEmail(valeur(valeurs, colonnes, "email"));
        patient.setGroupeSanguin(valeur(valeurs, colonnes, "groupesanguin"));
        patient.setAllergies(valeur(valeurs, colonnes, "allergies"));
        patient.setAntecedentsMedicaux(valeur(valeurs, colonnes, "antecedentsmedicaux"));
        patient.setPersonneAContacter(valeur(valeurs, colonnes, "personneacontacter"));
        patient.setTelephonePersonneAContacter(valeur(valeurs, colonnes, "telephonepersonneacontacter"));
        return patient;
    }

    private Map<String, Integer> lireEntetes(Row ligne, DataFormatter formatter) {
        Map<String, Integer> colonnes = new HashMap<>();
        for (int i = 0; i < ligne.getLastCellNum(); i++) {
            colonnes.put(normaliser(formatter.formatCellValue(ligne.getCell(i))), i);
        }
        return colonnes;
    }

    private Map<String, Integer> lireEntetes(List<String> entetes) {
        Map<String, Integer> colonnes = new HashMap<>();
        for (int i = 0; i < entetes.size(); i++) colonnes.put(normaliser(entetes.get(i)), i);
        return colonnes;
    }

    private void verifierColonnesObligatoires(Map<String, Integer> colonnes) {
        if (!colonnes.containsKey("nom") || !colonnes.containsKey("prenom")) {
            throw new IllegalArgumentException("Les colonnes nom et prenom sont obligatoires");
        }
    }

    private String valeur(List<String> valeurs, Map<String, Integer> colonnes, String colonne) {
        Integer index = colonnes.get(colonne);
        return index != null && index < valeurs.size() ? valeurs.get(index).trim() : "";
    }

    private LocalDate lireDate(String valeur) {
        if (valeur.isBlank()) return null;
        for (DateTimeFormatter format : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy"))) {
            try { return LocalDate.parse(valeur, format); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("dateNaissance invalide (AAAA-MM-JJ ou JJ/MM/AAAA)");
    }

    private String normaliser(String texte) {
        return Normalizer.normalize(texte == null ? "" : texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String cleIdentite(String nom, String prenom) {
        return normaliser(nom) + "|" + normaliser(prenom);
    }

    @Audite(action = TypeActionAudit.MODIFICATION, entite = "Patient")
    public Patient update(Long id, Patient donnees) {
        Patient patient = findById(id);
        patient.setNom(donnees.getNom());
        patient.setPrenom(donnees.getPrenom());
        patient.setDateNaissance(donnees.getDateNaissance());
        patient.setSexe(donnees.getSexe());
        patient.setAdresse(donnees.getAdresse());
        patient.setTelephone(donnees.getTelephone());
        patient.setEmail(donnees.getEmail());
        patient.setGroupeSanguin(donnees.getGroupeSanguin());
        patient.setAllergies(donnees.getAllergies());
        patient.setAntecedentsMedicaux(donnees.getAntecedentsMedicaux());
        patient.setPersonneAContacter(donnees.getPersonneAContacter());
        patient.setTelephonePersonneAContacter(donnees.getTelephonePersonneAContacter());
        return patientRepository.save(patient);
    }

    @Audite(action = TypeActionAudit.SUPPRESSION, entite = "Patient")
    public void delete(Long id) {
        patientRepository.deleteById(id);
    }

    private String genererNumeroDossier() {
        long count = patientRepository.count() + 1;
        String candidat;
        do {
            candidat = "DOS-" + Year.now().getValue() + "-" + String.format("%05d", count);
            count++;
        } while (patientRepository.existsByNumeroDossier(candidat));
        return candidat;
    }
}
