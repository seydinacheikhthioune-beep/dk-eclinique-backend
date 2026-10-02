package com.eclinique.service;

import com.eclinique.dto.CreanceOrganismeResponse;
import com.eclinique.exception.ResourceNotFoundException;
import com.eclinique.model.Encaissement;
import com.eclinique.model.FactureOrganisme;
import com.eclinique.model.Organisme;
import com.eclinique.model.StatutFacture;
import com.eclinique.repository.EncaissementRepository;
import com.eclinique.repository.FactureOrganismeRepository;
import com.eclinique.repository.OrganismeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganismeService {
    private final OrganismeRepository organismeRepository;
    private final EncaissementRepository encaissementRepository;
    private final FactureOrganismeRepository factureOrganismeRepository;

    @Transactional(readOnly = true)
    public List<Organisme> findAll(boolean actifsSeulement) {
        return actifsSeulement ? organismeRepository.findByActifTrueOrderByNomAsc() : organismeRepository.findAllByOrderByNomAsc();
    }

    public Organisme findById(Long id) {
        return organismeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisme introuvable : " + id));
    }

    public Organisme create(Organisme organisme) {
        if (organismeRepository.existsByNomIgnoreCase(organisme.getNom().trim())) {
            throw new IllegalArgumentException("Un organisme nommé " + organisme.getNom() + " existe déjà");
        }
        organisme.setId(null);
        organisme.setNom(organisme.getNom().trim());
        return organismeRepository.save(organisme);
    }

    public Organisme update(Long id, Organisme donnees) {
        Organisme organisme = findById(id);
        organisme.setNom(donnees.getNom().trim());
        organisme.setType(donnees.getType());
        organisme.setTauxPriseEnCharge(donnees.getTauxPriseEnCharge());
        organisme.setAdresse(donnees.getAdresse());
        organisme.setTelephone(donnees.getTelephone());
        organisme.setEmail(donnees.getEmail());
        organisme.setNinea(donnees.getNinea());
        organisme.setContact(donnees.getContact());
        organisme.setActif(donnees.isActif());
        return organismeRepository.save(organisme);
    }

    @Transactional(readOnly = true)
    public List<CreanceOrganismeResponse> creances() {
        Map<Long, FactureOrganisme> factures = factureOrganismeRepository.findAll().stream()
                .collect(Collectors.toMap(FactureOrganisme::getId, Function.identity()));
        Map<Long, List<Encaissement>> parOrganisme = encaissementRepository.findByOrganismeIdIsNotNull().stream()
                .collect(Collectors.groupingBy(Encaissement::getOrganismeId));
        return organismeRepository.findAllByOrderByNomAsc().stream().map(o -> {
            List<Encaissement> prises = parOrganisme.getOrDefault(o.getId(), List.of());
            double nonFacture = 0, enAttente = 0, paye = 0;
            for (Encaissement e : prises) {
                double part = e.partOrganismeEffective();
                FactureOrganisme f = e.getFactureOrganismeId() == null ? null : factures.get(e.getFactureOrganismeId());
                if (f == null) nonFacture += part;
                else if (f.getStatut() == StatutFacture.PAYEE) paye += part;
                else enAttente += part;
            }
            return new CreanceOrganismeResponse(o.getId(), o.getNom(), o.getType(), prises.size(),
                    nonFacture + enAttente + paye, nonFacture, enAttente, paye);
        }).toList();
    }
}
