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

package org.eclipse.edc.protocol.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A persistable representation of a dataspace profile. It is the durable projection of a
 * {@link DataspaceProfileContext}: it holds only serializable data (protocol version, binding,
 * namespace, JSON-LD context urls) and omits the live function objects ({@code webhook},
 * {@code idExtractionFunction}) which are resolved at registration time.
 * <p>
 * Instances of this type are stored in the {@code DataspaceProfileStore} and, on boot or on
 * creation through the management API, converted to a {@link DataspaceProfileContext} and
 * registered into the {@link DataspaceProfileContextRegistry}.
 */
@JsonDeserialize(builder = DataspaceProfile.Builder.class)
public class DataspaceProfile {

    private String name;
    private String protocolVersion;
    private String path;
    private String binding;
    private String namespace;
    private List<String> jsonLdContextsUrl = new ArrayList<>();
    private final List<TrustedIssuer> trustedIssuers = new ArrayList<>();
    private long createdAt;

    private DataspaceProfile() {
    }

    /**
     * The profile identifier. Doubles as the primary key of the entity.
     */
    public String getName() {
        return name;
    }

    /**
     * The DSP protocol version this profile binds to.
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * The protocol path. May be null/blank, in which case {@code "/" + name} is derived at conversion time.
     */
    public String getPath() {
        return path;
    }

    /**
     * The protocol binding (e.g. {@code https}).
     */
    public String getBinding() {
        return binding;
    }

    /**
     * The JSON-LD namespace of the dataspace profile.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The JSON-LD context document urls used for compaction.
     */
    public List<String> getJsonLdContextsUrl() {
        return jsonLdContextsUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .protocolVersion(protocolVersion)
                .path(path)
                .binding(binding)
                .namespace(namespace)
                .jsonLdContextsUrl(jsonLdContextsUrl)
                .createdAt(createdAt);
    }

    public List<TrustedIssuer> getTrustedIssuers() {
        return trustedIssuers;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final DataspaceProfile entity = new DataspaceProfile();
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            entity.name = name;
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            entity.protocolVersion = protocolVersion;
            return this;
        }

        public Builder path(String path) {
            entity.path = path;
            return this;
        }

        public Builder binding(String binding) {
            entity.binding = binding;
            return this;
        }

        public Builder namespace(String namespace) {
            entity.namespace = namespace;
            return this;
        }

        public Builder jsonLdContextsUrl(List<String> jsonLdContextsUrl) {
            entity.jsonLdContextsUrl = jsonLdContextsUrl == null ? new ArrayList<>() : new ArrayList<>(jsonLdContextsUrl);
            return this;
        }

        public Builder createdAt(long createdAt) {
            entity.createdAt = createdAt;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder trustedIssuers(List<TrustedIssuer> trustedIssuers) {
            if (trustedIssuers != null) {
                entity.trustedIssuers.addAll(trustedIssuers);
            }
            return this;
        }

        public DataspaceProfile build() {
            Objects.requireNonNull(entity.name, "Dataspace profile name cannot be null");
            if (entity.createdAt == 0) {
                entity.createdAt = clock.millis();
            }
            return entity;
        }
    }
}
