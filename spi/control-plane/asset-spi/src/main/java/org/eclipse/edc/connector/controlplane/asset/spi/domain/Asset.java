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

package org.eclipse.edc.connector.controlplane.asset.spi.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


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
    public static final String PROPERTY_IS_CATALOG = EDC_NAMESPACE + "isCatalog";
    public static final String EDC_ASSET_TYPE_TERM = "Asset";
    public static final String EDC_ASSET_TYPE = EDC_NAMESPACE + EDC_ASSET_TYPE_TERM;
    public static final String EDC_CATALOG_ASSET_TYPE_TERM = "CatalogAsset";
    public static final String EDC_CATALOG_ASSET_TYPE = EDC_NAMESPACE + EDC_CATALOG_ASSET_TYPE_TERM;
    public static final String EDC_ASSET_PROPERTIES = EDC_NAMESPACE + "properties";
    public static final String EDC_ASSET_PRIVATE_PROPERTIES = EDC_NAMESPACE + "privateProperties";
    public static final String EDC_ASSET_DATA_ADDRESS = EDC_NAMESPACE + "dataAddress";
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> privateProperties = new HashMap<>();
    private DataAddress dataAddress;

    private Asset() {
    }

    @Override
    public String getId() {
        return id == null ? ofNullable(getPropertyAsString(PROPERTY_ID)).orElse(getPropertyAsString(id)) : id;
    }

    @JsonIgnore
    public String getName() {
        return ofNullable(getPropertyAsString(PROPERTY_NAME))
                .orElse(getPropertyAsString("name"));
    }

    @JsonIgnore
    public String getDescription() {
        return ofNullable(getPropertyAsString(PROPERTY_DESCRIPTION)).orElse(getPropertyAsString("description"));
    }

    @JsonIgnore
    public String getVersion() {
        return ofNullable(getPropertyAsString(PROPERTY_VERSION)).orElse(getPropertyAsString("version"));
    }

    @JsonIgnore
    public String getContentType() {
        return ofNullable(getPropertyAsString(PROPERTY_CONTENT_TYPE)).orElse(getPropertyAsString("contenttype"));
    }

    @JsonIgnore
    public boolean isCatalog() {
        return ofNullable(getPropertyAsString(PROPERTY_IS_CATALOG))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonIgnore
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @JsonIgnore
    public Object getPropertyOrPrivate(String key) {
        return properties.getOrDefault(key, privateProperties.get(key));
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public Object getPrivateProperty(String key) {
        return privateProperties.get(key);
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public Builder toBuilder() {
        return Asset.Builder.newInstance()
                .id(id)
                .properties(properties)
                .privateProperties(privateProperties)
                .dataAddress(dataAddress)
                .createdAt(createdAt);
    }

    @JsonIgnore
    public boolean hasDuplicatePropertyKeys() {
        var properties = getProperties();
        var privateProperties = getPrivateProperties();
        if (privateProperties != null && properties != null) {
            return privateProperties.keySet().stream().distinct().anyMatch(properties::containsKey);
        }
        return true;
    }

    private String getPropertyAsString(String key) {
        var val = getProperty(key);
        return val != null ? val.toString() : null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Entity.Builder<Asset, Builder> {

        protected Builder(Asset asset) {
            super(asset);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new Asset());
        }

        @Override
        public Builder id(String id) {
            entity.id = id;
            entity.properties.put(PROPERTY_ID, id);
            return self();
        }

        @Override
        public Builder createdAt(long value) {
            entity.createdAt = value;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Asset build() {
            super.build();

            if (entity.getId() == null) {
                id(UUID.randomUUID().toString());
            }

            return entity;
        }

        public Builder name(String title) {
            entity.properties.put(PROPERTY_NAME, title);
            return self();
        }

        public Builder description(String description) {
            entity.properties.put(PROPERTY_DESCRIPTION, description);
            return self();
        }

        public Builder version(String version) {
            entity.properties.put(PROPERTY_VERSION, version);
            return self();
        }

        public Builder contentType(String contentType) {
            entity.properties.put(PROPERTY_CONTENT_TYPE, contentType);
            return self();
        }

        public Builder properties(Map<String, Object> properties) {
            Objects.requireNonNull(properties);
            entity.properties.putAll(properties);
            return self();
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return self();
        }

        public Builder dataAddress(DataAddress dataAddress) {
            entity.dataAddress = dataAddress;
            return self();
        }

        public Builder privateProperties(Map<String, Object> privateProperties) {
            Objects.requireNonNull(privateProperties);
            entity.privateProperties.putAll(privateProperties);
            return self();
        }

        public Builder privateProperty(String key, Object value) {
            entity.privateProperties.put(key, value);
            return self();
        }
    }

}
