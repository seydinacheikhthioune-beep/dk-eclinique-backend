package com.eclinique.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:utilisateur IS NULL OR LOWER(a.utilisateurUsername) LIKE LOWER(CONCAT('%', :utilisateur, '%'))) AND " +
            "(:entite IS NULL OR a.entite = :entite) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:debut IS NULL OR a.dateAction >= :debut) AND " +
            "(:fin IS NULL OR a.dateAction <= :fin)")
    Page<AuditLog> rechercher(@Param("utilisateur") String utilisateur,
                               @Param("entite") String entite,
                               @Param("action") TypeActionAudit action,
                               @Param("debut") LocalDateTime debut,
                               @Param("fin") LocalDateTime fin,
                               Pageable pageable);

    @Query("SELECT DISTINCT a.entite FROM AuditLog a ORDER BY a.entite")
    List<String> listerEntitesDistinctes();
}
