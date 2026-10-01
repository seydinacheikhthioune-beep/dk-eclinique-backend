package com.eclinique.service;

import com.eclinique.dto.EncaissementResponse;
import com.eclinique.model.Encaissement;
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

    public Encaissement enregistrer(Patient patient, TypeEncaissement type, Double montant, Long medecinId,
                                    String typeConsultation, Long rendezVousId, UtilisateurPrincipal auteur) {
        if (montant == null || montant < 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
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
                .enregistreParId(enregistrePar == null ? null : enregistrePar.getId())
                .enregistreParNom(enregistrePar == null ? null : enregistrePar.getPrenom() + " " + enregistrePar.getNom())
                .build());
    }

    @Transactional(readOnly = true)
    public List<EncaissementResponse> findAll() {
        return encaissementRepository.findAllByOrderByDateEncaissementDesc().stream().map(e ->
                new EncaissementResponse(e.getId(), e.getType(), e.getMontant(), e.getPatientId(), e.getPatientNom(),
                        e.getNumeroDossier(), e.getMedecinNom(), e.getTypeConsultation(), e.getEnregistreParNom(),
                        e.getDateEncaissement())).toList();
    }
}
