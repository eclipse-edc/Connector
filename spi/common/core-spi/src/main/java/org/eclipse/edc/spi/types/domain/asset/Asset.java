/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.spi.types.domain.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;


/**
 * The {@link Asset} contains the metadata and describes the data itself or a collection of data.
 */
@JsonDeserialize(builder = Asset.Builder.class)
public class Asset extends Entity {

    public static final String PROPERTY_ID = EDC_NAMESPACE + "id";
    public static final String PROPERTY_NAME = EDC_NAMESPACE + "name";
    public static final String PROPERTY_DESCRIPTION = EDC_NAMESPACE + "description";
    public static final String PROPERTY_VERSION = EDC_NAMESPACE + "version";
    public static final String PROPERTY_CONTENT_TYPE = EDC_NAMESPACE + "contenttype";

    @Deprecated(since = "milestone9")
    private static final String DEPRECATED_PROPERTY_PREFIX = "asset:prop:";

    private final Map<String, Object> properties;
    private final Map<String, Object> privateProperties;

    protected Asset() {
        properties = new HashMap<>();
        privateProperties = new HashMap<>();
    }

    @Override
    public String getId() {
        return id == null ? ofNullable(getPropertyAsString(PROPERTY_ID)).orElse(getPropertyAsString(DEPRECATED_PROPERTY_PREFIX + id)) : id;
    }

    @JsonIgnore
    public String getName() {
        return ofNullable(getPropertyAsString(PROPERTY_NAME))
                .orElse(getPropertyAsString(DEPRECATED_PROPERTY_PREFIX + "name"));
    }

    @JsonIgnore
    public String getDescription() {
        return ofNullable(getPropertyAsString(PROPERTY_DESCRIPTION)).orElse(getPropertyAsString(DEPRECATED_PROPERTY_PREFIX + "description"));
    }

    @JsonIgnore
    public String getVersion() {
        return ofNullable(getPropertyAsString(PROPERTY_VERSION)).orElse(getPropertyAsString(DEPRECATED_PROPERTY_PREFIX + "version"));
    }

    @JsonIgnore
    public String getContentType() {
        return ofNullable(getPropertyAsString(PROPERTY_CONTENT_TYPE)).orElse(getPropertyAsString(DEPRECATED_PROPERTY_PREFIX + "contenttype"));
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonIgnore
    public Object getProperty(String key) {
        return properties.get(key);
    }

    private String getPropertyAsString(String key) {
        var val = getProperty(key);
        return val != null ? val.toString() : null;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public Object getPrivateProperty(String key) {
        return privateProperties.get(key);
    }

    private String getPrivatePropertyAsString(String key) {
        var val = getPrivateProperty(key);
        return val != null ? val.toString() : null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<B extends Builder<B>> extends Entity.Builder<Asset, Builder<B>> {

        protected Builder(Asset asset) {
            super(asset);
        }

        @JsonCreator
        public static <B extends Builder<B>> Builder<B> newInstance() {
            return new Builder<>(new Asset());
        }

        @Override
        public B id(String id) {
            // todo: remove storing the ID in the properties map in future versions
            entity.properties.put(PROPERTY_ID, id);
            entity.id = id;
            return self();
        }

        @Override
        public Builder<B> createdAt(long value) {
            entity.createdAt = value;
            return self();
        }

        @Override
        public B self() {
            return (B) this;
        }

        @Override
        public Asset build() {
            if (entity.getId() == null) {
                id(UUID.randomUUID().toString());
            }
            return super.build();
        }

        public B name(String title) {
            entity.properties.put(PROPERTY_NAME, title);
            return self();
        }

        public B description(String description) {
            entity.properties.put(PROPERTY_DESCRIPTION, description);
            return self();
        }

        public B version(String version) {
            entity.properties.put(PROPERTY_VERSION, version);
            return self();
        }

        public B contentType(String contentType) {
            entity.properties.put(PROPERTY_CONTENT_TYPE, contentType);
            return self();
        }

        public B properties(Map<String, Object> properties) {
            Objects.requireNonNull(properties);
            entity.properties.putAll(properties);
            return self();
        }

        public B property(String key, Object value) {
            entity.properties.put(key, value);
            return self();
        }

        public B privateProperties(Map<String, Object> privateProperties) {
            Objects.requireNonNull(privateProperties);
            entity.privateProperties.putAll(privateProperties);
            return self();
        }

        public B privateProperty(String key, Object value) {
            entity.privateProperties.put(key, value);
            return self();
        }
    }

}
