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
    private Long rendezVousId;

    private Long enregistreParId;
    private String enregistreParNom;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dateEncaissement = LocalDateTime.now();
}
