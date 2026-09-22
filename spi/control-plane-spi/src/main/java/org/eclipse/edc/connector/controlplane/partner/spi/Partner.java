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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * A {@link Partner} is a counterparty known to a participant context. It binds the counterparty {@code identity}
 * (the id the dataspace profile extracts from the verified token, e.g. a DID) to a set of {@link PartnerGroup}s and
 * to free-form {@code properties} that can hold additional business identifiers (e.g. a customer or supplier number).
 * <p>
 * Partners are looked up during policy evaluation, so a policy can restrict access to counterparties that belong to
 * a given group or carry a given property.
 */
@JsonDeserialize(builder = Partner.Builder.class)
public class Partner extends AbstractParticipantResource {

    public static final String EDC_PARTNER_TYPE_TERM = "Partner";
    public static final String EDC_PARTNER_TYPE_IRI = EDC_NAMESPACE + EDC_PARTNER_TYPE_TERM;
    public static final String EDC_PARTNER_IDENTITY_TERM = "identity";
    public static final String EDC_PARTNER_IDENTITY_IRI = EDC_NAMESPACE + EDC_PARTNER_IDENTITY_TERM;
    public static final String EDC_PARTNER_NAME_TERM = "name";
    public static final String EDC_PARTNER_NAME_IRI = EDC_NAMESPACE + EDC_PARTNER_NAME_TERM;
    public static final String EDC_PARTNER_PROPERTIES_TERM = "properties";
    public static final String EDC_PARTNER_PROPERTIES_IRI = EDC_NAMESPACE + EDC_PARTNER_PROPERTIES_TERM;
    public static final String EDC_PARTNER_GROUP_IDS_TERM = "groupIds";
    public static final String EDC_PARTNER_GROUP_IDS_IRI = EDC_NAMESPACE + EDC_PARTNER_GROUP_IDS_TERM;

    private final Map<String, Object> properties = new HashMap<>();
    private final Set<String> groupIds = new HashSet<>();
    private String identity;
    private String name;

    private Partner() {
    }

    /**
     * The identity of the counterparty, as extracted from the verified protocol token (e.g. a DID).
     */
    @NotNull
    public String getIdentity() {
        return identity;
    }

    public String getName() {
        return name;
    }

    /**
     * Free-form properties. Business identifiers such as a customer number can be stored here and queried with nested criteria,
     * e.g. {@code properties.businessId = BID-0001}.
     */
    @NotNull
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Ids of the {@link PartnerGroup}s this partner belongs to. Groups belong to the same participant context.
     */
    @NotNull
    public Set<String> getGroupIds() {
        return Collections.unmodifiableSet(groupIds);
    }

    public boolean isInGroup(String groupId) {
        return groupIds.contains(groupId);
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .participantContextId(participantContextId)
                .createdAt(createdAt)
                .clock(clock)
                .identity(identity)
                .name(name)
                .properties(properties)
                .groupIds(groupIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (Partner) o;
        return Objects.equals(id, that.id) && Objects.equals(participantContextId, that.participantContextId) &&
                Objects.equals(identity, that.identity) && Objects.equals(name, that.name) &&
                Objects.equals(properties, that.properties) && Objects.equals(groupIds, that.groupIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, participantContextId, identity, name, properties, groupIds);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AbstractParticipantResource.Builder<Partner, Builder> {

        private Builder() {
            super(new Partner());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder identity(String identity) {
            entity.identity = identity;
            return this;
        }

        public Builder name(String name) {
            entity.name = name;
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

        public Builder groupId(String groupId) {
            entity.groupIds.add(groupId);
            return this;
        }

        public Builder groupIds(Set<String> groupIds) {
            entity.groupIds.clear();
            if (groupIds != null) {
                entity.groupIds.addAll(groupIds);
            }
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Partner build() {
            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(entity.identity, "Partner identity cannot be null");
            return super.build();
        }
    }
}
