package com.eclinique.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Montant perçu à l'accueil lors de l'enregistrement d'un patient (consultation ou rendez-vous).
 * Les noms sont conservés en clair pour que l'historique comptable survive à la suppression d'un patient.
 */
@Entity
@Table(name = "encaissements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Encaissement {
    /** Les encaissements antérieurs à la prise en charge n'ont pas de part patient : tout était payé comptant. */
    public double partPatientEffective() { return partPatient != null ? partPatient : montant; }
    public double partOrganismeEffective() { return partOrganisme != null ? partOrganisme : 0; }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeEncaissement type;

    @Column(nullable = false)
    private Double montant;

    private Long patientId;
    private String patientNom;
    private String numeroDossier;

    private Long medecinId;
    private String medecinNom;

    private String typeConsultation;

    /** Prise en charge par un assureur / une IPM : partOrganisme + partPatient = montant. */
    private Long organismeId;
    private String organismeNom;
    private Double partOrganisme;
    private Double partPatient;
    private String matriculeAssure;
    /** Renseigné quand la prise en charge a été incluse dans une facture organisme. */
    private Long factureOrganismeId;
    private Long rendezVousId;

    private Long enregistreParId;
    private String enregistreParNom;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dateEncaissement = LocalDateTime.now();
}
