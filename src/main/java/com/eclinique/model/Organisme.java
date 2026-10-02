package com.eclinique.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Assureur (OLEA, AMSA...) ou IPM d'entreprise (ICS, GCO...) facturé pour les prises en charge. */
@Entity
@Table(name = "organismes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organisme {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false, unique = true)
    private String nom;

    @NotNull(message = "Le type est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeOrganisme type;

    /** Pourcentage pris en charge par défaut (ex : 80 pour 80 %). */
    @Min(0) @Max(100)
    @Builder.Default
    private Double tauxPriseEnCharge = 80.0;

    private String adresse;
    private String telephone;
    private String email;
    private String ninea;
    private String contact;

    @Builder.Default
    private boolean actif = true;
}
