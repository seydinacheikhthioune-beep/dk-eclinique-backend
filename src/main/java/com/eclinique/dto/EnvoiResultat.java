package com.eclinique.dto;

/** Résultat de l'envoi d'un document : succès, ou motif pour lequel il n'est pas parti. */
public record EnvoiResultat(Long id, String reference, String destinataire, String destinataires, boolean envoye,
                            String message) {
}
