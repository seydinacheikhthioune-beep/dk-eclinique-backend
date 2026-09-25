package com.eclinique.service;

import com.eclinique.audit.Audite;
import com.eclinique.audit.TypeActionAudit;
import com.eclinique.dto.UtilisateurRequest;
import com.eclinique.exception.BusinessException;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.Utilisateur;
import com.eclinique.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    public List<Utilisateur> findByRole(com.eclinique.model.Role role) {
        return utilisateurRepository.findByRole(role);
    }

    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }

    @Audite(action = TypeActionAudit.CREATION, entite = "Utilisateur")
    public Utilisateur create(UtilisateurRequest req) {
        if (utilisateurRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException("Ce nom d'utilisateur existe déjà");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && utilisateurRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Cet email existe déjà dans la base");
        }
        Utilisateur u = Utilisateur.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .telephone(req.getTelephone())
                .profession(req.getProfession())
                .anneesExperience(req.getAnneesExperience())
                .tauxHoraire(req.getTauxHoraire())
                .role(req.getRole())
                .actif(req.getActif() == null || req.getActif())
                .build();
        return utilisateurRepository.save(u);
    }

    @Transactional(readOnly = true)
    public boolean emailExiste(String email) {
        return email != null && !email.isBlank() && utilisateurRepository.existsByEmail(email.trim());
    }

    @Audite(action = TypeActionAudit.MODIFICATION, entite = "Utilisateur")
    public Utilisateur update(Long id, UtilisateurRequest req) {
        Utilisateur u = findById(id);
        u.setEmail(req.getEmail());
        u.setNom(req.getNom());
        u.setPrenom(req.getPrenom());
        u.setTelephone(req.getTelephone());
        u.setProfession(req.getProfession());
        u.setAnneesExperience(req.getAnneesExperience());
        u.setTauxHoraire(req.getTauxHoraire());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getActif() != null) u.setActif(req.getActif());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return utilisateurRepository.save(u);
    }

    public Page<Utilisateur> findAllPage(Pageable pageable) {
        Pageable ordreDecroissant = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        return utilisateurRepository.findAll(ordreDecroissant);
    }

    @Audite(action = TypeActionAudit.SUPPRESSION, entite = "Utilisateur")
    public void delete(Long id) {
        utilisateurRepository.deleteById(id);
    }
}
