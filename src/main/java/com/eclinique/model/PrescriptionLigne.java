package com.eclinique.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_lignes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrescriptionLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicament_id", nullable = false)
    private Medicament medicament;

    @Column(nullable = false)
    private Integer quantite;

    private String posologie;
    private String dureeTraitement;
}
