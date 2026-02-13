/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.poker.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Configuration for serving Next.js static export files. Serves files from
 * /app/webapp/ (Docker) or classpath (local dev).
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve Next.js static files from /app/webapp/ (Docker deployment)
        // Skip /api paths to avoid interfering with REST endpoints
        registry.addResourceHandler("/**").addResourceLocations("file:/app/webapp/")
                .addResourceLocations("classpath:/static/").addResourceLocations("classpath:/public/")
                .resourceChain(true).addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        // If file exists, serve it
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }

                        // Skip API paths
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // For Next.js static export: try appending index.html to the path
                        // e.g., "online/available/" -> "online/available/index.html"
                        if (resourcePath.endsWith("/") || !resourcePath.contains(".")) {
                            String indexPath = resourcePath.endsWith("/")
                                    ? resourcePath + "index.html"
                                    : resourcePath + "/index.html";
                            Resource indexResource = location.createRelative(indexPath);
                            if (indexResource.exists() && indexResource.isReadable()) {
                                return indexResource;
                            }
                        }

                        // Final fallback: root index.html for SPA routing
                        Resource rootIndex = location.createRelative("index.html");
                        if (rootIndex.exists() && rootIndex.isReadable()) {
                            return rootIndex;
                        }

                        return null;
                    }
                });
    }
}
