package com.eclinique.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicament_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Medicament medicament;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMouvementStock type;

    @Column(nullable = false)
    private Integer quantite;

    private String motif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Builder.Default
    private LocalDateTime dateMouvement = LocalDateTime.now();

    private Integer stockApresMouvement;
}
