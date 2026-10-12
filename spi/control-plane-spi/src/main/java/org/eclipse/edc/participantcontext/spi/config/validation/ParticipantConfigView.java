/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.participantcontext.spi.config.validation;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only view of the effective configuration of a participant context, as it would be persisted after a write.
 * <p>
 * Only the values of public entries are exposed: stored private entries are encrypted, so conditions evaluated on a
 * view (see {@link ParticipantConfigEntry#isRequired(ParticipantConfigView)}) can only depend on public values and on
 * the presence of keys.
 */
public final class ParticipantConfigView {

    private final Map<String, String> entries;
    private final Set<String> privateKeys;

    private ParticipantConfigView(Map<String, String> entries, Set<String> privateKeys) {
        this.entries = entries;
        this.privateKeys = privateKeys;
    }

    public static ParticipantConfigView of(ParticipantContextConfiguration configuration) {
        return new ParticipantConfigView(Map.copyOf(configuration.getEntries()), Set.copyOf(configuration.getPrivateEntries().keySet()));
    }

    /**
     * Whether the key is present, either as a public or as a private entry.
     */
    public boolean hasKey(String key) {
        return entries.containsKey(key) || privateKeys.contains(key);
    }

    /**
     * Returns the value of a public entry, empty if the key is absent or is a private entry.
     */
    public Optional<String> getEntry(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    /**
     * Whether the key is present as a private entry.
     */
    public boolean isPrivate(String key) {
        return privateKeys.contains(key);
    }

    public Set<String> keys() {
        var keys = new HashSet<>(entries.keySet());
        keys.addAll(privateKeys);
        return keys;
    }
}
