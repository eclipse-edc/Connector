/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.assetindex.azure.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeName("dataspaceconnector:assetdocument")
public class AssetDocument extends CosmosDocument<Asset> {
    private final String id;
    private final Map<String, Object> sanitizedProperties;

    @JsonCreator
    public AssetDocument(@JsonProperty("wrappedInstance") Asset wrappedInstance,
                         @JsonProperty("partitionKey") String partitionKey) {
        super(wrappedInstance, partitionKey);
        id = wrappedInstance.getId();
        sanitizedProperties = sanitizeProperties(wrappedInstance);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getSanitizedProperties() {
        return sanitizedProperties;
    }

    private static Map<String, Object> sanitizeProperties(Asset asset) {
        return asset.getProperties().entrySet().stream()
                .collect(Collectors.toMap(entry -> sanitizeKey(entry.getKey()), Map.Entry::getValue));
    }

    private static String sanitizeKey(String key) {
        return key.replace(':', '_');
    }
}
