package com.eclinique.audit;

import java.lang.annotation.*;

/**
 * Annotation à poser sur une méthode de service pour que l'action soit
 * automatiquement consignée dans le journal d'audit par {@link AuditAspect}.
 *
 * Exemple :
 * <pre>
 *   {@literal @}Audite(action = TypeActionAudit.CREATION, entite = "Patient")
 *   public Patient create(Patient patient) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audite {
    TypeActionAudit action();
    String entite();
}
