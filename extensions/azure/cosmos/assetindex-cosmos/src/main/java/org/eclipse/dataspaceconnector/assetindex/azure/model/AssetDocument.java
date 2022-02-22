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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeName("dataspaceconnector:assetdocument")
public class AssetDocument extends CosmosDocument<Map<String, Object>> {
    private final String id;
    private final DataAddress dataAddress;

    public AssetDocument(Asset wrappedInstance,
                         String partitionKey,
                         DataAddress dataAddress) {
        super(sanitizeProperties(wrappedInstance), partitionKey);
        id = wrappedInstance.getId();
        this.dataAddress = dataAddress;
    }

    @JsonCreator
    public AssetDocument(@JsonProperty("wrappedInstance") Map<String, Object> wrappedInstance,
                         @JsonProperty("partitionKey") String partitionKey,
                         @JsonProperty("dataAddress") DataAddress dataAddress) {
        super(wrappedInstance, partitionKey);
        id = wrappedInstance.get("asset_prop_id").toString();
        this.dataAddress = dataAddress;
    }


    private static Map<String, Object> sanitizeProperties(Asset asset) {
        return asset.getProperties().entrySet().stream()
                .collect(Collectors.toMap(entry -> sanitize(entry.getKey()), Map.Entry::getValue));
    }

    @Override
    public String getId() {
        return id;
    }

    @JsonIgnore
    public Asset getWrappedAsset() {
        return Asset.Builder.newInstance()
                .id(id)
                .properties(restoreProperties())
                .build();
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    private Map<String, Object> restoreProperties() {
        var map = getWrappedInstance();

        return map.entrySet().stream().collect(Collectors.toMap(entry -> unsanitize(entry.getKey()), Map.Entry::getValue));
    }

    private String unsanitize(String key) {
        return key.replace("_", ":");
    }
}
