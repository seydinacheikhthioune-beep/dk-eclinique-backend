package com.eclinique.controller;

import com.eclinique.model.Medicament;
import com.eclinique.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicaments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MedicamentController {

    private final StockService stockService;

    @GetMapping
    public List<Medicament> findAll() {
        return stockService.findAllMedicaments();
    }

    @GetMapping("/alertes")
    public List<Medicament> alertes() {
        return stockService.medicamentsEnAlerte();
    }

    @GetMapping("/bientot-perimes")
    public List<Medicament> bientotPerimes(@RequestParam(defaultValue = "30") int jours) {
        return stockService.medicamentsBientotPerimes(jours);
    }

    @GetMapping("/{id}")
    public Medicament findById(@PathVariable Long id) {
        return stockService.findMedicamentById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    public ResponseEntity<Medicament> create(@Valid @RequestBody Medicament medicament) {
        return ResponseEntity.ok(stockService.creerMedicament(medicament));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    public Medicament update(@PathVariable Long id, @Valid @RequestBody Medicament medicament) {
        return stockService.modifierMedicament(id, medicament);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stockService.supprimerMedicament(id);
        return ResponseEntity.noContent().build();
    }
}
