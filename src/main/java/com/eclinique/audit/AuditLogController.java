package com.eclinique.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoint de consultation du journal d'audit. Réservé aux administrateurs :
 * l'audit trace notamment des actions sensibles (paiements, suppressions, connexions...).
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public Page<AuditLog> rechercher(
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) TypeActionAudit action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {

        Pageable pageable = PageRequest.of(page, taille, Sort.by(Sort.Direction.DESC, "dateAction"));
        return auditLogService.rechercher(utilisateur, entite, action, debut, fin, pageable);
    }

    @GetMapping("/entites")
    public List<String> entitesDisponibles() {
        return auditLogService.listerEntites();
    }

    @GetMapping("/actions")
    public TypeActionAudit[] actionsDisponibles() {
        return TypeActionAudit.values();
    }
}
