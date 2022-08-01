/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.ids.jsonld.type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.net.URI;

/**
 * Custom Jackson serializer for objects of type URI.
 */
public class UriSerializer extends StdSerializer<URI> {

    public UriSerializer() {
        super(URI.class);
    }

    @Override
    public void serialize(URI value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        var serializedUri = value.toString();

        var context = gen.getOutputContext();
        if (context.getCurrentName() != null && context.getCurrentName().contains("@id")) {
            gen.writeString(serializedUri);
        } else {
            gen.writeStartObject();
            gen.writeStringField("@id", serializedUri);
            gen.writeEndObject();
        }
    }
}
