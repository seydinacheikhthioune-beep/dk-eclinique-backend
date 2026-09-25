package com.eclinique.controller;

import com.eclinique.dto.NotificationResponse;
import com.eclinique.security.UtilisateurPrincipal;
import com.eclinique.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> findAll(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        return notificationService.pourUtilisateur(principal.getId());
    }

    @GetMapping("/non-lues/count")
    public long compterNonLues(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        return notificationService.compterNonLues(principal.getId());
    }

    @PatchMapping("/{id}/lue")
    public void marquerLue(@PathVariable Long id, @AuthenticationPrincipal UtilisateurPrincipal principal) {
        notificationService.marquerLue(id, principal.getId());
    }
}