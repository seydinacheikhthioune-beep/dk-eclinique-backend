package com.eclinique.controller;

import com.eclinique.dto.UtilisateurRequest;
import com.eclinique.model.Role;
import com.eclinique.model.Utilisateur;
import com.eclinique.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    public List<Utilisateur> findAll() {
        return utilisateurService.findAll();
    }

    @GetMapping("/medecins")
    @PreAuthorize("isAuthenticated()")
    public List<Utilisateur> findMedecins() {
        return utilisateurService.findByRole(Role.MEDECIN);
    }

    @GetMapping("/employes")
    public List<Utilisateur> findEmployes() {
        return utilisateurService.findAll().stream()
                .filter(utilisateur -> utilisateur.getRole() != Role.ADMIN)
                .toList();
    }

    @GetMapping("/employes/page")
    public Page<Utilisateur> findEmployesPage(Pageable pageable) {
        return utilisateurService.findAllPage(pageable);
    }

    @GetMapping("/email-existe")
    public boolean emailExiste(@RequestParam String email) {
        return utilisateurService.emailExiste(email);
    }

    @GetMapping("/{id}")
    public Utilisateur findById(@PathVariable Long id) {
        return utilisateurService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Utilisateur> create(@Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.create(request));
    }

    @PutMapping("/{id}")
    public Utilisateur update(@PathVariable Long id, @Valid @RequestBody UtilisateurRequest request) {
        return utilisateurService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        utilisateurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
