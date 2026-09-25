package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.exception.BusinessException;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.*;
import com.eclinique.repository.MedicamentRepository;
import com.eclinique.repository.MouvementStockRepository;
import com.eclinique.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;

/**
 * Service central de gestion automatisée des stocks de médicaments.
 * Toute variation de stock (entrée manuelle, sortie manuelle, ou sortie
 * automatique suite à une prescription) transite par ce service afin de
 * garantir la cohérence du stock et la traçabilité (MouvementStock).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final MedicamentRepository medicamentRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final UtilisateurRepository utilisateurRepository;

    public List<Medicament> findAllMedicaments() {
        return medicamentRepository.findAll().stream()
            .sorted(Comparator.comparing(Medicament::getDateCreation,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public Medicament findMedicamentById(Long id) {
        return medicamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable : " + id));
    }

    @Audite(action = TypeActionAudit.CREATION, entite = "Medicament")
    public Medicament creerMedicament(Medicament medicament) {
        if (medicamentRepository.existsByCode(medicament.getCode())) {
            throw new BusinessException("Un médicament avec ce code existe déjà");
        }
        return medicamentRepository.save(medicament);
    }

    @Audite(action = TypeActionAudit.MODIFICATION, entite = "Medicament")
    public Medicament modifierMedicament(Long id, Medicament donnees) {
        Medicament m = findMedicamentById(id);
        m.setNom(donnees.getNom());
        m.setCategorie(donnees.getCategorie());
        m.setForme(donnees.getForme());
        m.setUnite(donnees.getUnite());
        m.setDescription(donnees.getDescription());
        m.setPrixUnitaire(donnees.getPrixUnitaire());
        m.setSeuilAlerte(donnees.getSeuilAlerte());
        m.setDateExpiration(donnees.getDateExpiration());
        m.setFournisseur(donnees.getFournisseur());
        return medicamentRepository.save(m);
    }

    @Audite(action = TypeActionAudit.SUPPRESSION, entite = "Medicament")
    public void supprimerMedicament(Long id) {
        medicamentRepository.deleteById(id);
    }

    public List<Medicament> medicamentsEnAlerte() {
        return medicamentRepository.findMedicamentsEnAlerte();
    }

    public List<Medicament> medicamentsBientotPerimes(int joursAvant) {
        return medicamentRepository.findMedicamentsBientotPerimes(LocalDate.now().plusDays(joursAvant));
    }

    public List<MouvementStock> historiqueMouvements(Long medicamentId) {
        if (medicamentId != null) {
            return mouvementStockRepository.findByMedicamentIdOrderByDateMouvementDesc(medicamentId);
        }
        return mouvementStockRepository.findAllByOrderByDateMouvementDesc();
    }

    /** Mouvement manuel (entrée d'un approvisionnement, ajustement, ou sortie manuelle). */
    @Audite(action = TypeActionAudit.MOUVEMENT_STOCK, entite = "MouvementStock")
    public MouvementStock enregistrerMouvement(Long medicamentId, TypeMouvementStock type, int quantite,
                                                String motif, Long utilisateurId) {
        Medicament medicament = findMedicamentById(medicamentId);
        return appliquerMouvement(medicament, type, quantite, motif, utilisateurId);
    }

    /**
     * Sortie automatique de stock déclenchée par une prescription médicale
     * (appelée par ConsultationService). Lève une exception si le stock est insuffisant.
     */
    @Audite(action = TypeActionAudit.MOUVEMENT_STOCK, entite = "MouvementStock")
    public void sortieAutomatiquePourPrescription(Medicament medicament, int quantite, Long utilisateurId) {
        if (medicament.getQuantiteStock() < quantite) {
            throw new BusinessException("Stock insuffisant pour " + medicament.getNom()
                    + " (disponible : " + medicament.getQuantiteStock() + ", demandé : " + quantite + ")");
        }
        appliquerMouvement(medicament, TypeMouvementStock.SORTIE, quantite,
                "Sortie automatique - prescription médicale", utilisateurId);
    }

    private MouvementStock appliquerMouvement(Medicament medicament, TypeMouvementStock type, int quantite,
                                               String motif, Long utilisateurId) {
        int nouveauStock;
        switch (type) {
            case ENTREE -> nouveauStock = medicament.getQuantiteStock() + quantite;
            case SORTIE -> {
                if (medicament.getQuantiteStock() < quantite) {
                    throw new BusinessException("Stock insuffisant pour " + medicament.getNom());
                }
                nouveauStock = medicament.getQuantiteStock() - quantite;
            }
            case AJUSTEMENT -> nouveauStock = quantite; // valeur absolue du nouveau stock
            default -> throw new BusinessException("Type de mouvement inconnu");
        }

        medicament.setQuantiteStock(nouveauStock);
        medicamentRepository.save(medicament);

        Utilisateur utilisateur = utilisateurId != null ? utilisateurRepository.findById(utilisateurId).orElse(null) : null;

        MouvementStock mouvement = MouvementStock.builder()
                .medicament(medicament)
                .type(type)
                .quantite(quantite)
                .motif(motif)
                .utilisateur(utilisateur)
                .stockApresMouvement(nouveauStock)
                .build();
        return mouvementStockRepository.save(mouvement);
    }
}
