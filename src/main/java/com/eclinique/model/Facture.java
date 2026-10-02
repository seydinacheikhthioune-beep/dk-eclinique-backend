package com.eclinique.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroFacture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"consultations", "rendezVous"})
    private Patient patient;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    @Builder.Default
    private LocalDateTime dateFacture = LocalDateTime.now();

    private LocalDateTime dateEcheance;
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutFacture statut = StatutFacture.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;

    @Column(nullable = false)
    @Builder.Default
    private Double montantTotal = 0.0;

    @Column(length = 2000)
    private String observations;

    private Double remise;

    private LocalDate dateAdmission;
    private LocalDate dateSortie;
    private Double prixJournalierHospitalisation;
    private Integer joursHospitalisation;

    /** Tiers-payant : organisme qui règle partOrganisme ; le patient ne paie que partPatient. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organisme_id")
    private Organisme organisme;

    private String matriculeAssure;
    private Double tauxPriseEnCharge;
    private Double partOrganisme;
    private Double partPatient;

    /** Renseigné quand la part organisme a été incluse dans une facture organisme. */
    private Long factureOrganismeId;

    /** Les factures antérieures au tiers-payant sont entièrement à la charge du patient. */
    public double partOrganismeEffective() { return partOrganisme != null ? partOrganisme : 0; }
    public double partPatientEffective() {
        return partPatient != null ? partPatient : (montantTotal == null ? 0 : montantTotal);
    }

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneFacture> lignes = new ArrayList<>();
}
