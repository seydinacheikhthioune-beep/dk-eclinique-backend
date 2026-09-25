package com.eclinique.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Représente une entrée du dossier médical d'un patient (consultation).
 */
@Entity
@Table(name = "consultations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consultation {

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rendez_vous_id")
    @JsonIgnore
    private RendezVous rendezVous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TypeConsultation type = TypeConsultation.GENERALE;

    @Builder.Default
    private LocalDateTime dateConsultation = LocalDateTime.now();

    @Column(length = 2000)
    private String motif;

    @Column(length = 1000)
    private String symptomes;

    @Column(length = 2000)
    private String diagnostic;

    @Column(length = 2000)
    private String observations;

    private Double temperature;
    private Double tensionArterielle_systolique;
    private Double tensionArterielle_diastolique;
    private Double poids;
    private Double taille;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrescriptionLigne> prescriptions = new ArrayList<>();
}
