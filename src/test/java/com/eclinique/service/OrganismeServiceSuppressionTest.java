package com.eclinique.service;

import com.eclinique.exception.BusinessException;
import com.eclinique.model.Organisme;
import com.eclinique.model.Patient;
import com.eclinique.model.TypeOrganisme;
import com.eclinique.repository.OrganismeRepository;
import com.eclinique.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrganismeServiceSuppressionTest {

    @Autowired OrganismeService organismeService;
    @Autowired OrganismeRepository organismeRepository;
    @Autowired PatientRepository patientRepository;

    @Test
    void supprimeUnOrganismeInutilise() {
        Organisme ipm = organismeRepository.save(Organisme.builder().nom("IPM saisie par erreur").type(TypeOrganisme.IPM).build());

        organismeService.delete(ipm.getId());

        assertThat(organismeRepository.existsById(ipm.getId())).isFalse();
    }

    @Test
    void refuseLaSuppressionDUnOrganismeRattacheAUnPatient() {
        Organisme ipm = organismeRepository.save(Organisme.builder().nom("IPM utilisée").type(TypeOrganisme.IPM).build());
        patientRepository.save(Patient.builder().nom("Diop").prenom("Awa").numeroDossier("DOS-TEST-IPM").organisme(ipm).build());

        assertThatThrownBy(() -> organismeService.delete(ipm.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Désactivez-le");
        assertThat(organismeRepository.existsById(ipm.getId())).isTrue();
    }
}
