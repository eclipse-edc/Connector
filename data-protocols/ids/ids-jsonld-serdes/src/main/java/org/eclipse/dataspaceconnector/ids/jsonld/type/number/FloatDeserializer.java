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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;

/**
 * Custom Jackson deserializer for objects of type Float.
 */
public class FloatDeserializer extends StdDeserializer<Float> {

    public FloatDeserializer() {
        super(Float.class);
    }

    @Override
    public Float deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        try {
            return Float.valueOf(getValue(parser.readValueAsTree(), parser));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getValue(TreeNode node, JsonParser parser) throws JsonParseException {
        if (node instanceof TextNode) {
            return ((TextNode) node).textValue();
        }

        if (node instanceof ObjectNode) {
            return getValue(node.get("@value"), parser);
        }

        throw new JsonParseException(parser, "Could not read Float");
    }

}
