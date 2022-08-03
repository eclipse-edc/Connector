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

package org.eclipse.dataspaceconnector.ids.jsonld.type.number;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Custom Jackson serializer for objects of type Number.
 */
public class NumSerializer extends StdSerializer<Number> {

    public NumSerializer() {
        super(Number.class);
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("@value", value.toString());

        if (value instanceof BigDecimal) {
            gen.writeStringField("@type", "http://www.w3.org/2001/XMLSchema#decimal");
        } else if (value instanceof BigInteger) {
            gen.writeStringField("@type", "http://www.w3.org/2001/XMLSchema#integer");
        } else if (value instanceof Float) {
            gen.writeStringField("@type", "http://www.w3.org/2001/XMLSchema#float");
        } else if (value instanceof Long) {
            gen.writeStringField("@type", "http://www.w3.org/2001/XMLSchema#long");
        }

        gen.writeEndObject();
    }
}
