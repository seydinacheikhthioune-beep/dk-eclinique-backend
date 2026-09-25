package com.eclinique.audit;

import com.eclinique.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Point d'entrée unique pour écrire et consulter le journal d'audit.
 * L'écriture d'un log ne doit jamais faire échouer l'action métier en cours :
 * toute erreur est capturée et journalisée localement (log applicatif) sans être propagée.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /** Utilisé par l'aspect : récupère automatiquement l'utilisateur courant depuis le contexte de sécurité. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(TypeActionAudit action, String entite, Long entiteId, String description, boolean succes) {
        try {
            Long utilisateurId = null;
            String utilisateurUsername = "anonyme";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UtilisateurPrincipal principal) {
                utilisateurId = principal.getId();
                utilisateurUsername = principal.getUsername();
            } else if (auth != null && auth.getName() != null) {
                utilisateurUsername = auth.getName();
            }

            AuditLog auditLog = AuditLog.builder()
                    .utilisateurId(utilisateurId)
                    .utilisateurUsername(utilisateurUsername)
                    .action(action)
                    .entite(entite)
                    .entiteId(entiteId)
                    .description(description)
                    .adresseIp(recupererAdresseIp())
                    .succes(succes)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Échec de l'enregistrement du journal d'audit ({} / {}) : {}", action, entite, e.getMessage());
        }
    }

    /** Utilisé pour les événements de connexion/déconnexion, où l'utilisateur n'est pas (encore) authentifié dans le contexte. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrerAvecUtilisateur(String username, TypeActionAudit action, String entite,
                                            String description, boolean succes) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .utilisateurUsername(username == null ? "inconnu" : username)
                    .action(action)
                    .entite(entite)
                    .description(description)
                    .adresseIp(recupererAdresseIp())
                    .succes(succes)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Échec de l'enregistrement du journal d'audit ({} / {}) : {}", action, entite, e.getMessage());
        }
    }

    public Page<AuditLog> rechercher(String utilisateur, String entite, TypeActionAudit action,
                                      LocalDateTime debut, LocalDateTime fin, Pageable pageable) {
        return auditLogRepository.rechercher(vide(utilisateur), vide(entite), action, debut, fin, pageable);
    }

    public List<String> listerEntites() {
        return auditLogRepository.listerEntitesDistinctes();
    }

    private String vide(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur;
    }

    private String recupererAdresseIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
