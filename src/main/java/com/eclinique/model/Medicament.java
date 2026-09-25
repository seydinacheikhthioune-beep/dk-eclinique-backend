package com.eclinique.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicaments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Medicament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nom;

    private String categorie;
    private String forme; // comprimé, sirop, injection...
    private String unite; // boite, flacon, plaquette...

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double prixUnitaire;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantiteStock = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer seuilAlerte = 10;

    private LocalDate dateExpiration;

    private String fournisseur;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Transient
    public boolean isEnAlerte() {
        return quantiteStock != null && seuilAlerte != null && quantiteStock <= seuilAlerte;
    }
}
