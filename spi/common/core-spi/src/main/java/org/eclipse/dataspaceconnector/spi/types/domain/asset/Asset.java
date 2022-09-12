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

package org.eclipse.dataspaceconnector.spi.types.domain.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * The {@link Asset} contains the metadata and describes the data itself or a collection of data.
 */
@JsonDeserialize(builder = Asset.Builder.class)
public class Asset extends Entity {

    @Deprecated(since = "Do not use anymore. The ID should be accessed through the getter")
    public static final String PROPERTY_ID = "asset:prop:id";
    public static final String PROPERTY_NAME = "asset:prop:name";
    public static final String PROPERTY_DESCRIPTION = "asset:prop:description";
    public static final String PROPERTY_VERSION = "asset:prop:version";
    public static final String PROPERTY_CONTENT_TYPE = "asset:prop:contenttype";

    private final Map<String, Object> properties;

    protected Asset() {
        properties = new HashMap<>();
    }

    @Override
    public String getId() {
        return id == null ? getPropertyAsString(PROPERTY_ID) : id;
    }

    @JsonIgnore
    public String getName() {
        return getPropertyAsString(PROPERTY_NAME);
    }

    @JsonIgnore
    public String getDescription() {
        return getPropertyAsString(PROPERTY_DESCRIPTION);
    }

    @JsonIgnore
    public String getVersion() {
        return getPropertyAsString(PROPERTY_VERSION);
    }

    @JsonIgnore
    public String getContentType() {
        return getPropertyAsString(PROPERTY_CONTENT_TYPE);
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

    }

}
