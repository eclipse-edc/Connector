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

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO representation of an Asset.
 */
public class AssetDto extends Typed {

    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String id;
    private final Map<String, Object> properties;
    private final Map<String, Object> privateProperties;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Map<String, Object> dataplaneMetadata;

    public AssetDto(String id,
                    Map<String, Object> properties, Map<String, Object> privateProperties,
                    Map<String, Object> dataplaneMetadata) {
        super("Asset");
        this.id = id;
        this.properties = properties;
        this.privateProperties = privateProperties;
        this.dataplaneMetadata = dataplaneMetadata;
    }

    public AssetDto() {
        this(null, Map.of(), Map.of(), null);
    }

    public AssetDto(Map<String, Object> dataplaneMetadata) {
        this(null, Map.of(), Map.of(), dataplaneMetadata);
    }

    public AssetDto(Map<String, Object> properties, Map<String, Object> dataplaneMetadata) {
        this(null, properties, Map.of(), dataplaneMetadata);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public Map<String, Object> getDataplaneMetadata() {
        return dataplaneMetadata;
    }

}
