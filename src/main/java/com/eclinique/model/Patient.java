package com.eclinique.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consultation> consultations = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RendezVous> rendezVous = new ArrayList<>();
}
