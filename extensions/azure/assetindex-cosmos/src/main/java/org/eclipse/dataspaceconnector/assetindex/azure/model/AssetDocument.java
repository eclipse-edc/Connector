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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeName("dataspaceconnector:assetdocument")
public class AssetDocument extends CosmosDocument<Map<String, Object>> {
    private String id;
    private DataAddress dataAddress;

    protected AssetDocument() {
    }

    private AssetDocument(Map<String, Object> wrappedInstance, String partitionKey, DataAddress dataAddress) {
        super(sanitizeProperties(wrappedInstance), partitionKey);
        id = wrappedInstance.get("asset:prop:id").toString();
        this.dataAddress = dataAddress;
    }

    public static AssetDocument from(Asset wrappedInstance, String partitionKey, DataAddress dataAddress) {
        return new AssetDocument(wrappedInstance.getProperties(), partitionKey, dataAddress);
    }

    public static String sanitize(String key) {
        return key.replace(':', '_');
    }

    private static Map<String, Object> sanitizeProperties(Map<String, Object> properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(entry -> sanitize(entry.getKey()), Map.Entry::getValue));
    }

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