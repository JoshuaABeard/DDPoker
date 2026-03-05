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
package com.donohoedigital.games.poker.gameserver.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.dto.TemplateRequest;
import com.donohoedigital.games.poker.gameserver.dto.TemplateResponse;
import com.donohoedigital.games.poker.gameserver.persistence.repository.TournamentTemplateRepository;
import com.donohoedigital.games.poker.gameserver.service.ProfileService;
import com.donohoedigital.games.poker.model.TournamentTemplate;

/**
 * Template endpoints - CRUD operations for tournament configuration templates.
 */
@RestController
@RequestMapping("/api/v1/profiles/templates")
public class TemplateController {

    private final TournamentTemplateRepository templateRepository;
    private final ProfileService profileService;

    public TemplateController(TournamentTemplateRepository templateRepository, ProfileService profileService) {
        this.templateRepository = templateRepository;
        this.profileService = profileService;
    }

    /**
     * List all templates for the current user.
     */
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listTemplates() {
        Long profileId = getAuthenticatedProfileId();
        List<TemplateResponse> templates = templateRepository.findByProfileIdOrderByModifyDateDesc(profileId).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    /**
     * Create a new template for the current user.
     */
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@RequestBody TemplateRequest request) {
        Long profileId = getAuthenticatedProfileId();

        TournamentTemplate template = new TournamentTemplate();
        template.setProfileId(profileId);
        template.setName(request.name());
        template.setConfig(request.config());

        TournamentTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Update an existing template. Verifies the template belongs to the requesting
     * user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(@PathVariable("id") Long id,
            @RequestBody TemplateRequest request) {
        Long profileId = getAuthenticatedProfileId();

        Optional<TournamentTemplate> optionalTemplate = templateRepository.findById(id);
        if (optionalTemplate.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TournamentTemplate template = optionalTemplate.get();
        if (!template.getProfileId().equals(profileId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        template.setName(request.name());
        template.setConfig(request.config());

        TournamentTemplate saved = templateRepository.save(template);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Delete an existing template. Verifies the template belongs to the requesting
     * user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") Long id) {
        Long profileId = getAuthenticatedProfileId();

        Optional<TournamentTemplate> optionalTemplate = templateRepository.findById(id);
        if (optionalTemplate.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TournamentTemplate template = optionalTemplate.get();
        if (!template.getProfileId().equals(profileId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        templateRepository.delete(template);
        return ResponseEntity.noContent().build();
    }

    private TemplateResponse toResponse(TournamentTemplate template) {
        return new TemplateResponse(template.getId(), template.getName(), template.getConfig(),
                template.getCreateDate(), template.getModifyDate());
    }

    private Long getAuthenticatedProfileId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationFilter.JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getProfileId();
        }
        throw new IllegalStateException("No authenticated user found");
    }
}
