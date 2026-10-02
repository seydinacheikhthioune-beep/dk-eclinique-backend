package com.eclinique.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    private Sexe sexe;

    private String adresse;
    @Pattern(regexp = "^$|\\+?[0-9][0-9 .()\\-]{7,20}$", message = "Le numéro de téléphone est invalide")
    private String telephone;
    
    @Email(message = "L'adresse email est invalide")
    private String email;

    /** Numéro de dossier médical unique */
    @Column(unique = true)
    private String numeroDossier;

    private String groupeSanguin;

    @Column(length = 2000)
    private String allergies;

    @Column(length = 2000)
    private String antecedentsMedicaux;

    private String personneAContacter;
    @Pattern(regexp = "^$|\\+?[0-9][0-9 .()\\-]{7,20}$", message = "Le numéro de téléphone est invalide")
    private String telephonePersonneAContacter;

    /** Assureur ou IPM qui prend en charge le patient (null = paiement comptant). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organisme_id")
    private Organisme organisme;

    /** Matricule / numéro d'adhérent auprès de l'organisme. */
    private String matriculeAssure;

    /** Taux propre au patient (ex : ayant droit couvert à 70 %) ; null = taux par défaut de l'organisme. */
    @Min(0) @Max(100)
    private Double tauxPriseEnCharge;

    /** Date de fin de validité de la prise en charge ; null = sans limite. */
    private LocalDate dateFinCouverture;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    /** Taux appliqué au tiers-payant à la date donnée : 0 si pas d'organisme, organisme inactif ou couverture expirée. */
    public double tauxPriseEnChargeAu(LocalDate date) {
        if (organisme == null || !organisme.isActif()) return 0;
        if (dateFinCouverture != null && date.isAfter(dateFinCouverture)) return 0;
        Double taux = tauxPriseEnCharge != null ? tauxPriseEnCharge : organisme.getTauxPriseEnCharge();
        return taux == null ? 0 : taux;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consultation> consultations = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RendezVous> rendezVous = new ArrayList<>();
}
