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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * The {@link Asset} contains the metadata and describes the data itself or a collection of data.
 */
@JsonDeserialize(builder = Asset.Builder.class)
public class Asset {

    public static final String PROPERTY_ID = "asset:prop:id";
    public static final String PROPERTY_NAME = "asset:prop:name";
    public static final String PROPERTY_DESCRIPTION = "asset:prop:description";
    public static final String PROPERTY_VERSION = "asset:prop:version";
    public static final String PROPERTY_CONTENT_TYPE = "asset:prop:contenttype";

    private Map<String, Object> properties;

    protected Asset() {
        properties = new HashMap<>();
    }

    @JsonIgnore
    public String getId() {
        return getPropertyAsString(PROPERTY_ID);
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
    public static class Builder<B extends Builder<B>> {
        protected final Asset asset;

        protected Builder(Asset asset) {
            this.asset = asset;
            asset.properties.put(PROPERTY_ID, UUID.randomUUID().toString()); //must always have an ID
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new Asset());
        }

        public B id(String id) {
            asset.properties.put(PROPERTY_ID, id);
            return (B) this;
        }

        public B name(String title) {
            asset.properties.put(PROPERTY_NAME, title);
            return (B) this;
        }

        public B description(String description) {
            asset.properties.put(PROPERTY_DESCRIPTION, description);
            return (B) this;
        }

        public B version(String version) {
            asset.properties.put(PROPERTY_VERSION, version);
            return (B) this;
        }

        public B contentType(String contentType) {
            asset.properties.put(PROPERTY_CONTENT_TYPE, contentType);
            return (B) this;
        }

        public B properties(Map<String, Object> properties) {
            asset.properties = Objects.requireNonNull(properties);
            return (B) this;
        }

        public B property(String key, Object value) {
            asset.properties.put(key, value);
            return (B) this;
        }

        public Asset build() {
            return asset;
        }
    }

}
