package com.eclinique.service;

import com.eclinique.audit.AuditLogService;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.dto.JwtResponse;
import com.eclinique.dto.LoginRequest;
import com.eclinique.security.JwtUtils;
import com.eclinique.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;

    public JwtResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            String token = jwtUtils.generateToken(authentication);
            UtilisateurPrincipal principal = (UtilisateurPrincipal) authentication.getPrincipal();
            String role = principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

            auditLogService.enregistrerAvecUtilisateur(principal.getUsername(), TypeActionAudit.CONNEXION,
                    "Utilisateur", "Connexion réussie (rôle : " + role + ")", true);

            return new JwtResponse(token, principal.getId(), principal.getUsername(),
                    principal.getNom(), principal.getPrenom(), role);
        } catch (AuthenticationException e) {
            auditLogService.enregistrerAvecUtilisateur(request.getUsername(), TypeActionAudit.ECHEC_CONNEXION,
                    "Utilisateur", "Échec de connexion : identifiants invalides", false);
            throw e;
        }
    }

    /** Consigne la déconnexion explicite d'un utilisateur (appelé par /auth/logout). */
    public void logout(UtilisateurPrincipal principal) {
        auditLogService.enregistrerAvecUtilisateur(principal.getUsername(), TypeActionAudit.DECONNEXION,
                "Utilisateur", "Déconnexion", true);
    }
}
