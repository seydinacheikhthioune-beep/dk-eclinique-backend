package com.eclinique.service;

import com.eclinique.exception.BusinessException;
import com.eclinique.model.Notification;
import com.eclinique.model.Role;
import com.eclinique.model.Utilisateur;
import com.eclinique.repository.NotificationRepository;
import com.eclinique.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UtilisateurServiceSuppressionTest {

    @Autowired UtilisateurService utilisateurService;
    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired NotificationRepository notificationRepository;

    @Test
    void supprimeUnEmployeQuiADesNotifications() {
        Utilisateur employe = utilisateurRepository.save(Utilisateur.builder()
                .username("employe-a-supprimer").password("x").nom("Diop").prenom("Awa")
                .role(Role.EMPLOYE).actif(true).build());
        notificationRepository.save(Notification.builder().destinataire(employe).patientId(1L).message("Bonjour").build());

        utilisateurService.delete(employe.getId(), -1L);

        assertThat(utilisateurRepository.existsById(employe.getId())).isFalse();
        assertThat(notificationRepository.findByDestinataireIdOrderByDateCreationDesc(employe.getId())).isEmpty();
    }

    @Test
    void refuseLaSuppressionDeSonPropreCompte() {
        Utilisateur admin = utilisateurRepository.save(Utilisateur.builder()
                .username("admin-connecte").password("x").role(Role.ADMIN).actif(true).build());

        assertThatThrownBy(() -> utilisateurService.delete(admin.getId(), admin.getId()))
                .isInstanceOf(BusinessException.class);
    }
}
