/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.selector.instance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation for the {@link DataPlaneInstance} interface. Whether or not a request can be handled is solely
 * determined by evaluating the types of the source {@link DataAddress} and destination {@link DataAddress} and comparing them to a list of
 * "allowed" types for each.
 */
@JsonDeserialize(builder = DataPlaneInstanceImpl.Builder.class)
@JsonTypeName("dataspaceconnector:dataplaneinstance")
public class DataPlaneInstanceImpl implements DataPlaneInstance {

    private Map<String, Object> properties;
    @JsonProperty("allowedSourceTypes")
    private Set<String> allowedSourceTypes;
    @JsonProperty("allowedDestTypes")
    private Set<String> allowedDestTypes;
    private int turnCount;
    private long lastActive;
    private URL url;
    private String id;

    private DataPlaneInstanceImpl() {
        turnCount = 0;
        lastActive = Instant.now().toEpochMilli();
        properties = new HashMap<>();
        url = null;

        allowedSourceTypes = new HashSet<>();
        allowedDestTypes = new HashSet<>();
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Determines whether this instance can handle a particular source and data address, by evaluating {@link DataAddress#getType()}
     * against an internal list of allowed source and dest types.
     *
     * @param sourceAddress      The location where the data is located
     * @param destinationAddress The destination address of the data
     * @return true if can handle, false otherwise.
     */
    @Override
    public boolean canHandle(DataAddress sourceAddress, DataAddress destinationAddress) {
        Objects.requireNonNull(sourceAddress, "source cannot be null!");
        Objects.requireNonNull(destinationAddress, "destination cannot be null");
        return allowedSourceTypes.contains(sourceAddress.getType()) && allowedDestTypes.contains(destinationAddress.getType());
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public int getTurnCount() {
        return turnCount;
    }

    @Override
    public long getLastActive() {
        return lastActive;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final DataPlaneInstanceImpl instance;

        private Builder() {
            instance = new DataPlaneInstanceImpl();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder turnCount(int turnCount) {
            instance.turnCount = turnCount;
            return this;
        }

        public Builder lastActive(long lastActive) {
            instance.lastActive = lastActive;
            return this;
        }

        public Builder id(String id) {
            instance.id = id;
            return this;
        }

        public Builder allowedSourceType(String type) {
            instance.allowedSourceTypes.add(type);
            return this;
        }

        public Builder allowedDestType(String type) {
            instance.allowedDestTypes.add(type);
            return this;
        }

        public Builder url(URL url) {
            instance.url = url;
            return this;
        }

        public Builder url(String url) {
            try {
                instance.url = new URL(url);
            } catch (MalformedURLException e) {
                throw new EdcException(e);
            }
            return this;
        }

        public DataPlaneInstanceImpl build() {
            if (instance.id == null) {
                instance.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(instance.url, "DataPlaneInstance must have an URL");

            return instance;
        }

        public Builder property(String key, Object value) {
            instance.properties.put(key, value);
            return this;
        }

        // private to enable JSON deserialization, but not intended for direct use!
        private Builder allowedDestTypes(Set<String> types) {
            instance.allowedDestTypes = types;
            return this;
        }

        // private to enable JSON deserialization, but not intended for direct use!
        private Builder allowedSourceTypes(Set<String> types) {
            if (types != null) {
                instance.allowedSourceTypes = types;
            }
            return this;
        }

        private Builder properties(Map<String, Object> properties) {
            instance.properties = properties;
            return this;
        }
    }
}
