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
package com.donohoedigital.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * Adapter that implements java.util.prefs.Preferences interface and delegates to FilePrefs backend.
 * Enables zero-change compatibility with existing DDOption classes.
 *
 * <p>Node paths like "options/poker" are converted to dot-notation keys like "options.poker"
 * for storage in the flat FilePrefs structure.
 */
public class FilePrefsAdapter extends AbstractPreferences {
    private static final Logger logger = LogManager.getLogger(FilePrefsAdapter.class);

    private final String nodePath;

    /**
     * Create adapter for user root preferences
     */
    public FilePrefsAdapter() {
        this(null, "");
    }

    /**
     * Create adapter for specific node path
     */
    public FilePrefsAdapter(AbstractPreferences parent, String name) {
        super(parent, name);
        this.nodePath = buildNodePath(parent, name);
        logger.debug("Created FilePrefsAdapter for node: {}", nodePath);
    }

    /**
     * Build full node path from parent and name
     */
    private String buildNodePath(AbstractPreferences parent, String name) {
        if (parent == null || parent.absolutePath().equals("/")) {
            return name.isEmpty() ? "" : name;
        }
        String parentPath = parent.absolutePath().substring(1); // Remove leading /
        return name.isEmpty() ? parentPath : parentPath + "." + name;
    }

    /**
     * Convert node path and key to FilePrefs key.
     * Example: nodePath="options/poker", key="player.name" -> "options.poker.player.name"
     */
    private String toFilePrefsKey(String key) {
        if (nodePath.isEmpty()) {
            return key;
        }
        return nodePath.replace("/", ".") + "." + key;
    }

    @Override
    protected void putSpi(String key, String value) {
        String fullKey = toFilePrefsKey(key);
        logger.debug("Put: {} = {}", fullKey, value);
        FilePrefs.getInstance().put(fullKey, value);
    }

    @Override
    protected String getSpi(String key) {
        String fullKey = toFilePrefsKey(key);
        String value = FilePrefs.getInstance().get(fullKey, null);
        logger.debug("Get: {} = {}", fullKey, value);
        return value;
    }

    @Override
    protected void removeSpi(String key) {
        String fullKey = toFilePrefsKey(key);
        logger.debug("Remove: {}", fullKey);
        FilePrefs.getInstance().remove(fullKey);
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        // Remove all keys with this node's prefix
        logger.debug("Remove node: {}", nodePath);
        // FilePrefs doesn't have node concept, so we just clear keys with this prefix
        // For now, this is a no-op since we'd need to iterate all keys
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        // FilePrefs stores keys in a flat structure, so we can't easily enumerate
        // keys for a specific node. For now, return empty array.
        // This method is rarely used in practice - most code uses get() directly.
        logger.debug("Keys requested for node: {}", nodePath);
        return new String[0];
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        // FilePrefs doesn't have hierarchical nodes, so no children
        logger.debug("Children requested for node: {}", nodePath);
        return new String[0];
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        // Create child node adapter
        return new FilePrefsAdapter(this, name);
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        // FilePrefs flushes immediately on every change, so sync is a no-op
        logger.debug("Sync requested for node: {}", nodePath);
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        // FilePrefs flushes immediately on every change, so explicit flush is a no-op
        logger.debug("Flush requested for node: {}", nodePath);
        FilePrefs.getInstance().flush();
    }

    /**
     * Get the node path (for testing)
     */
    String getNodePath() {
        return nodePath;
    }
}
