/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;

import java.util.stream.Stream;

import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;

public final class DspError {

    public static JsonObject create(String type, String code, String... messages) {
        var arrayBuilder = Json.createArrayBuilder();

        Stream.of(messages).forEach(arrayBuilder::add);

        return Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, type)
                .add(DSPACE_PROPERTY_CODE, code)
                .add(DSPACE_PROPERTY_REASON, arrayBuilder.build())
                .build();
    }
}
