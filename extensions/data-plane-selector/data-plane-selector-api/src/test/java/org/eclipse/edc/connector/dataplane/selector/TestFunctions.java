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
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;

import java.util.Set;

import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

public class TestFunctions {

    public static DataPlaneInstance createInstance(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1")
                .build();
    }

    public static JsonObject createInstanceJson(String id) {
        return createInstanceJsonBuilder(id)
                .add(URL, "http://somewhere.com:1234/api/v1")
                .add(ALLOWED_SOURCE_TYPES, Json.createArrayBuilder(Set.of("source1", "source2")))
                .build();
    }

    public static JsonObjectBuilder createInstanceJsonBuilder(String id) {
        return Json.createObjectBuilder()
                .add(ID, id);
    }

    public static DataPlaneInstance.Builder createInstanceBuilder(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1");
    }
}
