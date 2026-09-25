package com.eclinique.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trace immuable d'une action effectuée dans l'application (création, modification,
 * suppression, connexion, mouvement de stock, paiement...). Le nom d'utilisateur est
 * dupliqué en clair (utilisateurUsername) afin que l'historique reste lisible même si
 * le compte utilisateur est supprimé par la suite.
 */
@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'utilisateur au moment de l'action (peut devenir orphelin). */
    private Long utilisateurId;

    /** Nom d'utilisateur figé au moment de l'action. */
    private String utilisateurUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeActionAudit action;

    /** Type d'entité concernée : Patient, RendezVous, Consultation, Medicament, Facture, Utilisateur... */
    @Column(nullable = false)
    private String entite;

    /** Identifiant de l'entité concernée, si applicable. */
    private Long entiteId;

    @Column(length = 1000)
    private String description;

    private String adresseIp;

    @Builder.Default
    private boolean succes = true;

    @Builder.Default
    private LocalDateTime dateAction = LocalDateTime.now();
}
