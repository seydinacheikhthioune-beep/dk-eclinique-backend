package com.eclinique.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "paiements_employes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaiementEmploye {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Utilisateur employe;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false, length = 40)
    private String periode;

    @Column(length = 500)
    private String motif;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime datePaiement = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effectue_par_id")
    private Utilisateur effectuePar;
}