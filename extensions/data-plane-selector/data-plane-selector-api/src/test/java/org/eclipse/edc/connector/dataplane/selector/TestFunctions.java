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

package org.eclipse.edc.connector.dataplane.selector;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.dataplane.selector.api.v2.model.SelectionRequest;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Set;

import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_DEST_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class TestFunctions {

    public static DataPlaneInstance createInstance(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1")
                .build();
    }

    public static JsonObject createInstanceJson(String id) {
        return Json.createObjectBuilder()
                .add(ID, id)
                .add(URL, "http://somewhere.com:1234/api/v1")
                .add(ALLOWED_SOURCE_TYPES, Json.createArrayBuilder(Set.of("source1", "source2")))
                .add(ALLOWED_DEST_TYPES, Json.createArrayBuilder(Set.of("dest1", "dest2")))
                .build();
    }

    public static JsonObject createSelectionRequestJson(String srcType, String destType, String strategy) {
        return Json.createObjectBuilder()
                .add(SelectionRequest.SOURCE_ADDRESS, createDataAddress(srcType))
                .add(SelectionRequest.DEST_ADDRESS, createDataAddress(destType))
                .add(SelectionRequest.STRATEGY, strategy)
                .build();
    }

    public static JsonObject createSelectionRequestJson(String srcType, String destType) {
        return Json.createObjectBuilder()
                .add(SelectionRequest.SOURCE_ADDRESS, createDataAddress(srcType))
                .add(SelectionRequest.DEST_ADDRESS, createDataAddress(destType))
                .build();
    }

    public static DataPlaneInstance.Builder createInstanceBuilder(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1");
    }

    private static JsonObjectBuilder createDataAddress(String type) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, type);
    }
}
