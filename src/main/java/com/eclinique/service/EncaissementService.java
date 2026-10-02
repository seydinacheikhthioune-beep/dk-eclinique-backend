package com.eclinique.service;

import com.eclinique.dto.EncaissementResponse;
import com.eclinique.model.Encaissement;
import com.eclinique.model.Organisme;
import com.eclinique.model.Patient;
import com.eclinique.model.TypeEncaissement;
import com.eclinique.model.Utilisateur;
import com.eclinique.repository.EncaissementRepository;
import com.eclinique.repository.UtilisateurRepository;
import com.eclinique.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EncaissementService {
    private final EncaissementRepository encaissementRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * @param partOrganisme part prise en charge par l'organisme du patient ; si null, elle est calculée
     *                      avec le taux par défaut de l'organisme. Ignorée si le patient n'a pas d'organisme.
     */
    public Encaissement enregistrer(Patient patient, TypeEncaissement type, Double montant, Long medecinId,
                                    String typeConsultation, Long rendezVousId, Double partOrganisme,
                                    UtilisateurPrincipal auteur) {
        if (montant == null || montant < 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        Organisme organisme = patient.getOrganisme();
        double priseEnCharge = 0;
        if (organisme != null) {
            priseEnCharge = partOrganisme != null ? partOrganisme
                    : Math.round(montant * valeur(organisme.getTauxPriseEnCharge()) / 100.0);
            if (priseEnCharge < 0 || priseEnCharge > montant) {
                throw new IllegalArgumentException("La part prise en charge doit être comprise entre 0 et le montant");
            }
        }
        Utilisateur medecin = medecinId == null ? null : utilisateurRepository.findById(medecinId).orElse(null);
        Utilisateur enregistrePar = auteur == null ? null : utilisateurRepository.findById(auteur.getId()).orElse(null);
        return encaissementRepository.save(Encaissement.builder()
                .type(type)
                .montant(montant)
                .patientId(patient.getId())
                .patientNom(patient.getPrenom() + " " + patient.getNom())
                .numeroDossier(patient.getNumeroDossier())
                .medecinId(medecinId)
                .medecinNom(medecin == null ? null : "Dr. " + medecin.getPrenom() + " " + medecin.getNom())
                .typeConsultation(type == TypeEncaissement.CONSULTATION ? typeConsultation : null)
                .rendezVousId(rendezVousId)
                .organismeId(organisme == null ? null : organisme.getId())
                .organismeNom(organisme == null ? null : organisme.getNom())
                .matriculeAssure(organisme == null ? null : patient.getMatriculeAssure())
                .partOrganisme(priseEnCharge)
                .partPatient(montant - priseEnCharge)
                .enregistreParId(enregistrePar == null ? null : enregistrePar.getId())
                .enregistreParNom(enregistrePar == null ? null : enregistrePar.getPrenom() + " " + enregistrePar.getNom())
                .build());
    }

    @Transactional(readOnly = true)
    public List<EncaissementResponse> findAll() {
        return encaissementRepository.findAllByOrderByDateEncaissementDesc().stream()
                .map(EncaissementResponse::of).toList();
    }

    private double valeur(Double nombre) { return nombre == null ? 0 : nombre; }
}
