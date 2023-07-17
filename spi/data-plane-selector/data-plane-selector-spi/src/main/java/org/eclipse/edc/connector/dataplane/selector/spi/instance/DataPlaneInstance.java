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

package org.eclipse.edc.connector.dataplane.selector.spi.instance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.DataAddress;

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

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Representations of a data plane instance. Every DPF has an ID and a URL as well as a number, how often it was selected,
 * and a timestamp of its last selection time. In addition, there are extensible properties to hold specific properties.
 */
public class DataPlaneInstance {

    public static final String DATAPLANE_INSTANCE_TYPE = EDC_NAMESPACE + "DataPlaneInstance";
    public static final String TURNCOUNT = EDC_NAMESPACE + "turnCount";
    public static final String LAST_ACTIVE = EDC_NAMESPACE + "lastActive";
    public static final String URL = EDC_NAMESPACE + "url";
    public static final String PROPERTIES = EDC_NAMESPACE + "properties";
    public static final String ALLOWED_SOURCE_TYPES = EDC_NAMESPACE + "allowedSourceTypes";
    public static final String ALLOWED_DEST_TYPES = EDC_NAMESPACE + "allowedDestTypes";

    private Map<String, Object> properties;

    private Set<String> allowedSourceTypes;

    private Set<String> allowedDestTypes;

    private int turnCount;

    private long lastActive;

    private URL url;

    private String id;

    protected DataPlaneInstance() {
        turnCount = 0;
        lastActive = Instant.now().toEpochMilli();
        properties = new HashMap<>();
        url = null;

        allowedSourceTypes = new HashSet<>();
        allowedDestTypes = new HashSet<>();
    }

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
    public boolean canHandle(DataAddress sourceAddress, DataAddress destinationAddress) {
        Objects.requireNonNull(sourceAddress, "source cannot be null!");
        Objects.requireNonNull(destinationAddress, "destination cannot be null");
        return allowedSourceTypes.contains(sourceAddress.getType()) && allowedDestTypes.contains(destinationAddress.getType());
    }

    public URL getUrl() {
        return url;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public long getLastActive() {
        return lastActive;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Set<String> getAllowedSourceTypes() {
        return Collections.unmodifiableSet(allowedSourceTypes);
    }

    public Set<String> getAllowedDestTypes() {
        return Collections.unmodifiableSet(allowedDestTypes);
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final DataPlaneInstance instance;

        private Builder() {
            instance = new DataPlaneInstance();
        }

        @JsonCreator
        public static DataPlaneInstance.Builder newInstance() {
            return new DataPlaneInstance.Builder();
        }

        public DataPlaneInstance.Builder turnCount(int turnCount) {
            instance.turnCount = turnCount;
            return this;
        }

        public DataPlaneInstance.Builder lastActive(long lastActive) {
            instance.lastActive = lastActive;
            return this;
        }

        public DataPlaneInstance.Builder id(String id) {
            instance.id = id;
            return this;
        }

        public DataPlaneInstance.Builder allowedSourceType(String type) {
            instance.allowedSourceTypes.add(type);
            return this;
        }

        public DataPlaneInstance.Builder allowedDestType(String type) {
            instance.allowedDestTypes.add(type);
            return this;
        }

        public DataPlaneInstance.Builder url(URL url) {
            instance.url = url;
            return this;
        }

        public DataPlaneInstance.Builder url(String url) {
            try {
                instance.url = new URL(url);
            } catch (MalformedURLException e) {
                throw new EdcException(e);
            }
            return this;
        }

        public DataPlaneInstance build() {
            if (instance.id == null) {
                instance.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(instance.url, "DataPlaneInstance must have an URL");

            return instance;
        }

        public DataPlaneInstance.Builder property(String key, Object value) {
            instance.properties.put(key, value);
            return this;
        }

        // private to enable JSON deserialization, but not intended for direct use!
        public DataPlaneInstance.Builder allowedDestTypes(Set<String> types) {
            instance.allowedDestTypes = types;
            return this;
        }

        // private to enable JSON deserialization, but not intended for direct use!
        public DataPlaneInstance.Builder allowedSourceTypes(Set<String> types) {
            if (types != null) {
                instance.allowedSourceTypes = types;
            }
            return this;
        }

        private DataPlaneInstance.Builder properties(Map<String, Object> properties) {
            instance.properties = properties;
            return this;
        }
    }
}
