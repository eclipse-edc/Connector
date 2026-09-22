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

package org.eclipse.edc.connector.controlplane.partner.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * A named group of {@link Partner}s owned by a participant context. Policies can restrict access to the members of a
 * group. Membership is recorded on the {@link Partner} through {@link Partner#getGroupIds()}.
 */
@JsonDeserialize(builder = PartnerGroup.Builder.class)
public class PartnerGroup extends AbstractParticipantResource {

    public static final String EDC_PARTNER_GROUP_TYPE_TERM = "PartnerGroup";
    public static final String EDC_PARTNER_GROUP_TYPE_IRI = EDC_NAMESPACE + EDC_PARTNER_GROUP_TYPE_TERM;
    public static final String EDC_PARTNER_GROUP_NAME_TERM = "name";
    public static final String EDC_PARTNER_GROUP_NAME_IRI = EDC_NAMESPACE + EDC_PARTNER_GROUP_NAME_TERM;
    public static final String EDC_PARTNER_GROUP_DESCRIPTION_TERM = "description";
    public static final String EDC_PARTNER_GROUP_DESCRIPTION_IRI = EDC_NAMESPACE + EDC_PARTNER_GROUP_DESCRIPTION_TERM;
    public static final String EDC_PARTNER_GROUP_PROPERTIES_TERM = "properties";
    public static final String EDC_PARTNER_GROUP_PROPERTIES_IRI = EDC_NAMESPACE + EDC_PARTNER_GROUP_PROPERTIES_TERM;

    private final Map<String, Object> properties = new HashMap<>();
    private String name;
    private String description;

    private PartnerGroup() {
    }

    @NotNull
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @NotNull
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .participantContextId(participantContextId)
                .createdAt(createdAt)
                .clock(clock)
                .name(name)
                .description(description)
                .properties(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PartnerGroup) o;
        return Objects.equals(id, that.id) && Objects.equals(participantContextId, that.participantContextId) &&
                Objects.equals(name, that.name) && Objects.equals(description, that.description) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, participantContextId, name, description, properties);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AbstractParticipantResource.Builder<PartnerGroup, Builder> {

        private Builder() {
            super(new PartnerGroup());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            entity.name = name;
            return this;
        }

        public Builder description(String description) {
            entity.description = description;
            return this;
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            entity.properties.clear();
            if (properties != null) {
                entity.properties.putAll(properties);
            }
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public PartnerGroup build() {
            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(entity.name, "PartnerGroup name cannot be null");
            return super.build();
        }
    }
}
