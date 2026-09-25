package com.eclinique.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Intercepte automatiquement les méthodes de service annotées {@link Audite}
 * pour écrire une entrée dans le journal d'audit, sans que les services aient
 * à appeler explicitement AuditLogService.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @AfterReturning(pointcut = "@annotation(audite)", returning = "resultat")
    public void apresExecutionReussie(JoinPoint joinPoint, Audite audite, Object resultat) {
        Long entiteId = extraireIdEntite(resultat, joinPoint.getArgs());
        String description = construireDescription(joinPoint, audite);
        auditLogService.enregistrer(audite.action(), audite.entite(), entiteId, description, true);
    }

    @AfterThrowing(pointcut = "@annotation(audite)", throwing = "exception")
    public void apresEchec(JoinPoint joinPoint, Audite audite, Throwable exception) {
        Long entiteId = extraireIdDepuisArguments(joinPoint.getArgs());
        String description = construireDescription(joinPoint, audite) + " — ÉCHEC : " + exception.getMessage();
        auditLogService.enregistrer(audite.action(), audite.entite(), entiteId, description, false);
    }

    private String construireDescription(JoinPoint joinPoint, Audite audite) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return audite.entite() + " — " + audite.action().name().toLowerCase().replace('_', ' ')
                + " (méthode : " + signature.getName() + ")";
    }

    /** Essaie d'abord de lire l'id sur l'objet retourné (getId()), sinon retombe sur les arguments de la méthode. */
    private Long extraireIdEntite(Object resultat, Object[] args) {
        Long id = extraireId(resultat);
        return id != null ? id : extraireIdDepuisArguments(args);
    }

    private Long extraireIdDepuisArguments(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        for (Object arg : args) {
            Long id = extraireId(arg);
            if (id != null) return id;
        }
        return null;
    }

    private Long extraireId(Object objet) {
        if (objet == null) return null;
        try {
            Method getId = objet.getClass().getMethod("getId");
            Object valeur = getId.invoke(objet);
            return valeur instanceof Long ? (Long) valeur : null;
        } catch (Exception e) {
            return null;
        }
    }
}
