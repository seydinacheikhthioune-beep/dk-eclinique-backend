package com.eclinique.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rendez_vous")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"consultations", "rendezVous"})
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Utilisateur medecin;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    private Integer dureeMinutes;

    private String motif;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutRendezVous statut = StatutRendezVous.PLANIFIE;

    @Column(length = 1000)
    private String notes;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
