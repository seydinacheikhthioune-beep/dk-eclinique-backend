package com.eclinique.audit;

/**
 * Types d'actions consignées dans le journal d'audit.
 */
public enum TypeActionAudit {
    CREATION,
    MODIFICATION,
    SUPPRESSION,
    CONSULTATION_DOSSIER,
    MOUVEMENT_STOCK,
    PAIEMENT,
    ANNULATION,
    CONNEXION,
    ECHEC_CONNEXION,
    DECONNEXION,
    AUTRE
}
