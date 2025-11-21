/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.spi.config.model;

import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class ParticipantContextConfiguration implements ParticipantResource {

    public static final String PARTICIPANT_CONTEXT_CONFIG_TYPE_TERM = "ParticipantContextConfig";
    public static final String PARTICIPANT_CONTEXT_CONFIG_TYPE_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_CONFIG_TYPE_TERM;
    public static final String PARTICIPANT_CONTEXT_CONFIG_ENTRIES_TERM = "entries";
    public static final String PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_CONFIG_ENTRIES_TERM;

    protected String participantContextId;
    protected Clock clock;
    protected long createdAt;
    private long lastModified;
    private Map<String, String> entries = new HashMap<>();

    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }

    @NotNull
    public Map<String, String> getEntries() {
        return entries;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastModified() {
        return lastModified;
    }

    public Builder toBuilder() {
        return Builder.newInstance()
                .participantContextId(participantContextId)
                .clock(clock)
                .createdAt(createdAt)
                .lastModified(lastModified)
                .entries(entries);
    }

    public static class Builder {
        private final ParticipantContextConfiguration configuration;

        private Builder() {
            configuration = new ParticipantContextConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder participantContextId(String participantContextId) {
            configuration.participantContextId = participantContextId;
            return this;
        }

        public Builder clock(Clock clock) {
            configuration.clock = clock;
            return this;
        }

        public Builder createdAt(long createdAt) {
            configuration.createdAt = createdAt;
            return this;
        }

        public Builder lastModified(long lastModified) {
            configuration.lastModified = lastModified;
            return this;
        }

        public Builder entries(Map<String, String> entries) {
            configuration.entries = entries;
            return this;
        }

        public Builder entry(String key, String value) {
            configuration.entries.put(key, value);
            return this;
        }

        public ParticipantContextConfiguration build() {
            Objects.requireNonNull(configuration.entries, "config must not be null");
            configuration.clock = Objects.requireNonNullElse(configuration.clock, Clock.systemUTC());

            if (configuration.createdAt == 0) {
                configuration.createdAt = configuration.clock.millis();
            }
            if (configuration.lastModified == 0) {
                configuration.lastModified = configuration.clock.millis();
            }
            return configuration;
        }
    }
}
