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


/**
 * The {@link Asset} contains the metadata and describes the data itself or a collection of data.
 */
@JsonDeserialize(builder = Asset.Builder.class)
public class Asset {

    private static final String PROP_KEY_ID = "asset:prop:key";
    private static final String PROP_KEY_NAME = "asset:prop:name";
    private static final String PROP_KEY_VERSION = "asset:prop:version";
    private static final String PROP_KEY_CONTENT_TYPE = "asset:prop:contenttype";
    private Map<String, Object> properties;

    private Asset() {
        properties = new HashMap<>();
    }

    @JsonIgnore
    public String getId() {
        return getPropertyAsString(PROP_KEY_ID);
    }

    @JsonIgnore
    public String getName() {
        return getPropertyAsString(PROP_KEY_NAME);
    }

    @JsonIgnore
    public String getVersion() {
        return getPropertyAsString(PROP_KEY_VERSION);
    }

    @JsonIgnore
    public String getContentType() {
        return getPropertyAsString(PROP_KEY_CONTENT_TYPE);
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
    public static final class Builder {
        private final Asset asset;

        private Builder() {
            asset = new Asset();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            asset.properties.put(PROP_KEY_ID, id);
            return this;
        }

        public Builder name(String title) {
            asset.properties.put(PROP_KEY_NAME, title);
            return this;
        }

        public Builder version(String version) {
            asset.properties.put(PROP_KEY_VERSION, version);
            return this;
        }

        public Builder contentType(String contentType) {
            asset.properties.put(PROP_KEY_CONTENT_TYPE, contentType);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            asset.properties = properties;
            return this;
        }

        public Builder property(String key, Object value) {
            asset.properties.put(key, value);
            return this;
        }

        public Asset build() {
            return asset;
        }
    }

}
