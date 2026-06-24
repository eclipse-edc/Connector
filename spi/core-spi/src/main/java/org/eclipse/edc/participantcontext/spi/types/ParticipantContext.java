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

package org.eclipse.edc.participantcontext.spi.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantContextState.CREATED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Representation of a participant in Identity Hub.
 */
@JsonDeserialize(builder = ParticipantContext.Builder.class)
public class ParticipantContext extends AbstractParticipantResource {
    public static final String PARTICIPANT_CONTEXT_TYPE_TERM = "ParticipantContext";
    public static final String PARTICIPANT_CONTEXT_TYPE_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_TYPE_TERM;
    public static final String PARTICIPANT_CONTEXT_IDENTITY_TERM = "identity";
    public static final String PARTICIPANT_CONTEXT_IDENTITY_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_IDENTITY_TERM;
    public static final String PARTICIPANT_CONTEXT_PROPERTIES_TERM = "properties";
    public static final String PARTICIPANT_CONTEXT_PROPERTIES_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_PROPERTIES_TERM;
    public static final String PARTICIPANT_CONTEXT_STATE_TERM = "state";
    public static final String PARTICIPANT_CONTEXT_STATE_IRI = EDC_NAMESPACE + PARTICIPANT_CONTEXT_STATE_TERM;

    protected String identity;
    protected Map<String, Object> properties = new HashMap<>();
    protected long lastModified;
    protected int state; // CREATED, ACTIVATED, DEACTIVATED

    protected ParticipantContext() {
    }

    /**
     * The unique identity for this ParticipantContext
     */
    public String getIdentity() {
        return identity;
    }

    /**
     * The POSIX timestamp in ms when this entry was last modified.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * The ParticipantContext's state. 0 = CREATED, 1 = ACTIVATED, 2 = DEACTIVATED
     */
    public int getState() {
        return state;
    }

    @JsonIgnore
    public ParticipantContextState getStateAsEnum() {
        return ParticipantContextState.from(state);
    }

    /**
     * Updates the last-modified field.
     */
    public void updateLastModified() {
        this.lastModified = Instant.now().toEpochMilli();
    }

    /**
     * Transitions this participant context to the {@link ParticipantContextState#ACTIVATED} state.
     */
    public void activate() {
        this.state = ParticipantContextState.ACTIVATED.code();
    }

    /**
     * Transitions this participant context to the {@link ParticipantContextState#DEACTIVATED} state.
     */
    public void deactivate() {
        this.state = ParticipantContextState.DEACTIVATED.code();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public ParticipantContext.Builder toBuilder() {
        return Builder.newInstance()
                .id(getId())
                .participantContextId(participantContextId)
                .createdAt(createdAt)
                .lastModified(lastModified)
                .state(getStateAsEnum())
                .properties(properties);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AbstractParticipantResource.Builder<ParticipantContext, Builder> {

        private Builder() {
            super(new ParticipantContext());
            entity.createdAt = Instant.now().toEpochMilli();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder lastModified(long lastModified) {
            this.entity.lastModified = lastModified;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ParticipantContext build() {
            Objects.requireNonNull(entity.participantContextId, "Participant Context ID cannot be null");
            Objects.requireNonNull(entity.identity, "identity cannot be null");

            if (entity.state == 0) {
                entity.state = CREATED.code();
            }
            if (entity.getLastModified() == 0L) {
                entity.lastModified = entity.getCreatedAt();
            }
            return super.build();
        }

        public Builder state(ParticipantContextState state) {
            this.entity.state = state.code();
            return this;
        }

        public Builder identity(String identity) {
            entity.identity = identity;
            return this;
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            entity.properties = properties;
            return this;
        }

    }
}