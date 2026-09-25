package com.eclinique.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Column(nullable = false, length = 500)
    private String message;

    @Column
    private Long consultationId;

    @Column
    private Long factureId;

    @Column
    private Long rendezVousId;

    private Long medecinId;
    private String medecinNom;
    private String medecinPrenom;
    private String typeConsultation;
    private Double montantConsultation;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "CONSULTATION";

    @Builder.Default
    @Column(nullable = false)
    private boolean lue = false;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}