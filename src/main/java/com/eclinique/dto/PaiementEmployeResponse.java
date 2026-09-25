package com.eclinique.dto;

import java.time.LocalDateTime;

public record PaiementEmployeResponse(Long id, Long employeId, String employeNom,
                                      Double montant, String periode, String motif,
                                      LocalDateTime datePaiement, Long effectueParId) {
}