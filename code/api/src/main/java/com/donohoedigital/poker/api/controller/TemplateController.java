/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.poker.api.controller;

import com.donohoedigital.games.poker.gameserver.persistence.repository.TournamentTemplateRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentTemplate;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.poker.api.dto.TemplateRequest;
import com.donohoedigital.poker.api.dto.TemplateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Template endpoints - CRUD operations for tournament configuration templates.
 */
@RestController
@RequestMapping("/api/profile/templates")
public class TemplateController {

    @Autowired
    private TournamentTemplateRepository templateRepository;

    @Autowired
    private OnlineProfileService profileService;

    /**
     * List all templates for the current user.
     */
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listTemplates(@AuthenticationPrincipal String username) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        List<TemplateResponse> templates = templateRepository.findByProfileIdOrderByModifyDateDesc(profile.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());

        return ResponseEntity.ok(templates);
    }

    /**
     * Create a new template for the current user.
     */
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@AuthenticationPrincipal String username,
            @RequestBody TemplateRequest request) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        TournamentTemplate template = new TournamentTemplate();
        template.setProfileId(profile.getId());
        template.setName(request.getName());
        template.setConfig(request.getConfig());

        TournamentTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Update an existing template. Verifies the template belongs to the requesting
     * user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(@AuthenticationPrincipal String username,
            @PathVariable Long id, @RequestBody TemplateRequest request) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<TournamentTemplate> optionalTemplate = templateRepository.findById(id);
        if (optionalTemplate.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TournamentTemplate template = optionalTemplate.get();
        if (!template.getProfileId().equals(profile.getId())) {
            return ResponseEntity.status(403).build();
        }

        template.setName(request.getName());
        template.setConfig(request.getConfig());

        TournamentTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Delete an existing template. Verifies the template belongs to the requesting
     * user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@AuthenticationPrincipal String username, @PathVariable Long id) {
        OnlineProfile profile = profileService.getOnlineProfileByName(username);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<TournamentTemplate> optionalTemplate = templateRepository.findById(id);
        if (optionalTemplate.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TournamentTemplate template = optionalTemplate.get();
        if (!template.getProfileId().equals(profile.getId())) {
            return ResponseEntity.status(403).build();
        }

        templateRepository.delete(template);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convert a TournamentTemplate entity to a TemplateResponse DTO.
     */
    private TemplateResponse toResponse(TournamentTemplate template) {
        return new TemplateResponse(template.getId(), template.getName(), template.getConfig(),
                template.getCreateDate(), template.getModifyDate());
    }
}
