package com.eclinique.config;

import com.eclinique.model.Role;
import com.eclinique.model.Patient;
import com.eclinique.model.Utilisateur;
import com.eclinique.repository.PatientRepository;
import com.eclinique.repository.UtilisateurRepository;
import com.eclinique.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialise un compte administrateur par défaut au premier démarrage,
 * afin de pouvoir se connecter immédiatement à l'application.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!utilisateurRepository.existsByUsername("admin")) {
            Utilisateur admin = Utilisateur.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .email("admin@eclinique.sn")
                    .nom("Administrateur")
                    .prenom("Systeme")
                    .role(Role.ADMIN)
                    .actif(true)
                    .build();
            utilisateurRepository.save(admin);
            System.out.println("=== Compte admin créé : username=admin / password=Admin@123 ===");
        }

        Utilisateur receptionniste = creerSiAbsent("mousampthioune@gmail.com", "moussa.reception", "Thioune", "Moussa", Role.RECEPTIONNISTE, "Reception@123");
        Utilisateur medecin1 = creerSiAbsent("eydina.cheikh.thioune@gmail.com", "dr.eydina", "Thioune", "Eydina Cheikh", Role.MEDECIN, "Medecin@123");
        creerSiAbsent("moussathioune2026@gmail.com", "dr.moussa", "Thioune", "Moussa", Role.MEDECIN, "Medecin@124");

        Patient patient = patientRepository.findByEmail("patient.demo@eclinique.sn").orElseGet(() ->
                patientRepository.save(Patient.builder()
                        .nom("Ndiaye")
                        .prenom("Aminata")
                        .email("patient.demo@eclinique.sn")
                        .telephone("+221 77 000 00 01")
                        .adresse("Dakar")
                        .numeroDossier("DOS-DEMO-00001")
                        .build()));
        if (receptionniste != null && medecin1 != null && patient.getId() != null) {
            notificationService.notifierNouveauPatient(patient, "CONSULTATION", medecin1.getId());
        }
    }

    private Utilisateur creerSiAbsent(String email, String username, String nom, String prenom,
                                      Role role, String password) {
        return utilisateurRepository.findByEmail(email).orElseGet(() -> utilisateurRepository.save(Utilisateur.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .nom(nom)
                .prenom(prenom)
                .role(role)
                .actif(true)
                .build()));
    }
}
