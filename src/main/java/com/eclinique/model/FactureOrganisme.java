package com.eclinique.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Facture adressée à un assureur ou une IPM, regroupant les prises en charge d'une période. */
@Entity
@Table(name = "factures_organismes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FactureOrganisme {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organisme_id", nullable = false)
    private Organisme organisme;

    @Column(nullable = false)
    private LocalDate periodeDebut;

    @Column(nullable = false)
    private LocalDate periodeFin;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dateEmission = LocalDateTime.now();

    @Column(nullable = false)
    private Double montantTotal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private StatutFacture statut = StatutFacture.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;

    private LocalDateTime datePaiement;

    @Column(length = 1000)
    private String observations;

    private String creeParNom;
}
