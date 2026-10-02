package com.eclinique.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Envoi d'un document par e-mail. Sans destinataires, on utilise l'adresse du concerné
 * (organisme, patient) ou celle du comptable pour un export. Les dates ne servent qu'à l'export comptable.
 */
@Data
public class EnvoiRequest {
    /** Adresses séparées par des virgules. */
    private String destinataires;
    private String message;
    private LocalDate debut;
    private LocalDate fin;
}
