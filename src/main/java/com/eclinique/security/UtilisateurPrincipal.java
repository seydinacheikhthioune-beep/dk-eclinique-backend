package com.eclinique.security;

import com.eclinique.model.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UtilisateurPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    @JsonIgnore
    private final String password;
    private final String email;
    private final String nom;
    private final String prenom;
    private final boolean actif;
    private final Collection<? extends GrantedAuthority> authorities;

    public UtilisateurPrincipal(Utilisateur u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.password = u.getPassword();
        this.email = u.getEmail();
        this.nom = u.getNom();
        this.prenom = u.getPrenom();
        this.actif = u.isActif();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return actif; }
}
