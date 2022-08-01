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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.net.URI;

/**
 * Custom Jackson deserializer for objects of type URI.
 */
public class UriDeserializer extends StdDeserializer<URI> {

    public UriDeserializer() {
        super(URI.class);
    }

    @Override
    public URI deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return URI.create(getValue(parser.readValueAsTree(), parser));
    }

    private String getValue(TreeNode node, JsonParser parser) throws JsonParseException {
        if (node instanceof TextNode) {
            return ((TextNode) node).textValue();
        }

        if (node instanceof ObjectNode) {
            return getValue(node.get("@id"), parser);
        }

        throw new JsonParseException(parser, "Could not read URI");
    }

}
