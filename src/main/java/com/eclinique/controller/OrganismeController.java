package com.eclinique.controller;

import com.eclinique.dto.CreanceOrganismeResponse;
import com.eclinique.model.Organisme;
import com.eclinique.service.OrganismeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organismes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrganismeController {
    private final OrganismeService service;

    @GetMapping
    public List<Organisme> findAll(@RequestParam(defaultValue = "false") boolean actifs) {
        return service.findAll(actifs);
    }

    @GetMapping("/creances")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE')")
    public List<CreanceOrganismeResponse> creances() { return service.creances(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Organisme create(@Valid @RequestBody Organisme organisme) { return service.create(organisme); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Organisme update(@PathVariable Long id, @Valid @RequestBody Organisme organisme) {
        return service.update(id, organisme);
    }
}
