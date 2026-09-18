/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.dataplane.spi.instance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.Entity;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates.UNREGISTERED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Representation of a data plane instance. Every data plane has an ID and a URL, the source and transfer types it
 * can handle, a registration {@link DataPlaneInstanceStates state} and extensible properties.
 */
public class DataPlaneInstance extends Entity implements ParticipantResource {

    public static final String DATAPLANE_INSTANCE_TYPE_TERM = "DataPlaneInstance";
    public static final String DATAPLANE_INSTANCE_TYPE = EDC_NAMESPACE + DATAPLANE_INSTANCE_TYPE_TERM;
    @Deprecated(since = "1.0.0")
    public static final String LAST_ACTIVE = EDC_NAMESPACE + "lastActive";
    public static final String URL = EDC_NAMESPACE + "url";
    public static final String PROPERTIES = EDC_NAMESPACE + "properties";
    public static final String ALLOWED_TRANSFER_TYPES = EDC_NAMESPACE + "allowedTransferTypes";
    public static final String ALLOWED_SOURCE_TYPES = EDC_NAMESPACE + "allowedSourceTypes";
    public static final String DESTINATION_PROVISION_TYPES = EDC_NAMESPACE + "destinationProvisionTypes";

    public static final String DATAPLANE_INSTANCE_STATE = EDC_NAMESPACE + "state";
    public static final String DATAPLANE_INSTANCE_STATE_TIMESTAMP = EDC_NAMESPACE + "stateTimestamp";
    private final Set<String> destinationProvisionTypes = new HashSet<>();
    private final Set<String> allowedTransferTypes = new HashSet<>();
    private final Set<String> allowedSourceTypes = new HashSet<>();
    private final Set<String> labels = new HashSet<>();
    private Map<String, Object> properties = new HashMap<>();
    private long lastActive = Instant.now().toEpochMilli();
    private URL url;
    private String participantContextId;
    private AuthorizationProfile authorizationProfile;
    private int state;
    private long stateTimestamp;
    private long updatedAt;

    private DataPlaneInstance() {
    }

    public Builder toBuilder() {
        return new Builder(copy());
    }

    public DataPlaneInstance copy() {
        return Builder.newInstance()
                .id(id)
                .createdAt(createdAt)
                .clock(clock)
                .updatedAt(updatedAt)
                .state(state)
                .stateTimestamp(stateTimestamp)
                .url(url)
                .lastActive(lastActive)
                .allowedSourceTypes(allowedSourceTypes)
                .allowedTransferType(allowedTransferTypes)
                .properties(properties)
                .destinationProvisionTypes(destinationProvisionTypes)
                .participantContextId(participantContextId)
                .labels(labels)
                .authorizationProfile(authorizationProfile)
                .build();
    }

    public String stateAsString() {
        return DataPlaneInstanceStates.from(state).name();
    }

    public int getState() {
        return state;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * Gets the last active timestamp of the data plane instance.
     *
     * @deprecated the last active timestamp is not tracked anymore and will be removed.
     */
    @Deprecated(since = "1.0.0")
    public long getLastActive() {
        return lastActive;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Set<String> getAllowedSourceTypes() {
        return Collections.unmodifiableSet(allowedSourceTypes);
    }

    public Set<String> getAllowedTransferTypes() {
        return Collections.unmodifiableSet(allowedTransferTypes);
    }

    public Set<String> getDestinationProvisionTypes() {
        return destinationProvisionTypes;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public AuthorizationProfile getAuthorizationProfile() {
        return authorizationProfile;
    }

    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }

    public void transitionToRegistered() {
        transitionTo(REGISTERED.code());
    }

    public void transitionToUnregistered() {
        transitionTo(UNREGISTERED.code());
    }

    private void transitionTo(int targetState) {
        state = targetState;
        stateTimestamp = clock.millis();
        updatedAt = clock.millis();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends Entity.Builder<DataPlaneInstance, Builder> {

        private Builder(DataPlaneInstance dataPlaneInstance) {
            super(dataPlaneInstance);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new DataPlaneInstance());
        }

        public Builder state(int state) {
            entity.state = state;
            return this;
        }

        public Builder stateTimestamp(long stateTimestamp) {
            entity.stateTimestamp = stateTimestamp;
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            entity.updatedAt = updatedAt;
            return this;
        }

        /**
         * Sets the last active timestamp of the data plane instance.
         *
         * @deprecated the last active timestamp is not tracked anymore and will be removed.
         */
        @Deprecated(since = "1.0.0")
        public Builder lastActive(long lastActive) {
            entity.lastActive = lastActive;
            return this;
        }

        public Builder allowedSourceType(String type) {
            entity.allowedSourceTypes.add(type);
            return this;
        }

        public Builder allowedTransferType(String type) {
            entity.allowedTransferTypes.add(type);
            return this;
        }

        public Builder url(URL url) {
            entity.url = url;
            return this;
        }

        public Builder url(String url) {
            try {
                entity.url = new URL(url);
            } catch (MalformedURLException e) {
                throw new EdcException(e);
            }
            return this;
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return this;
        }

        public Builder allowedSourceTypes(Set<String> types) {
            if (types != null) {
                entity.allowedSourceTypes.addAll(types);
            }
            return this;
        }

        public Builder allowedTransferType(Set<String> types) {
            if (types != null) {
                entity.allowedTransferTypes.addAll(types);
            }
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            entity.properties = properties;
            return this;
        }

        public Builder destinationProvisionTypes(Set<String> types) {
            if (types != null) {
                entity.destinationProvisionTypes.addAll(types);
            }
            return this;
        }

        public Builder participantContextId(String participantContextId) {
            entity.participantContextId = participantContextId;
            return this;
        }

        public Builder labels(Set<String> labels) {
            if (labels != null) {
                entity.labels.addAll(labels);
            }
            return this;
        }

        public Builder label(String label) {
            entity.labels.add(label);
            return this;
        }

        public Builder authorizationProfile(AuthorizationProfile authorizationProfile) {
            entity.authorizationProfile = authorizationProfile;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public DataPlaneInstance build() {
            Objects.requireNonNull(entity.url, "DataPlaneInstance must have an URL");

            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }

            super.build();

            if (entity.updatedAt == 0) {
                entity.updatedAt = entity.createdAt;
            }
            if (entity.stateTimestamp == 0) {
                entity.stateTimestamp = entity.clock.millis();
            }
            return entity;
        }
    }
}
