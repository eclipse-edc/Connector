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

package org.eclipse.edc.protocol.ids.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.net.URI;

public class CustomIdsConstraintDeserializer extends StdDeserializer<Constraint> {

    private static final long serialVersionUID = 1L;

    public CustomIdsConstraintDeserializer() {
        this(null);
    }

    public CustomIdsConstraintDeserializer(Class clazz) {
        super(clazz);
    }

    @Override
    public Constraint deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = p.getCodec().readTree(p);
        var id = getOrThrow(jsonNode, "@id");
        var leftOperand = getOrThrow(jsonNode, "ids:leftOperand");
        var operator = getOrThrow(jsonNode, "ids:operator");
        var rightOperand = jsonNode.get("ids:rightOperand");
        var rightOperandReference = jsonNode.get("ids:rightOperandReference");
        var unit = jsonNode.get("ids:unit");
        var endpoint = jsonNode.get("ids:pipEndpoint");
        // NOTE custom properties are not deserialized

        var builder = new IdsConstraintBuilder(URI.create(id.asText()))
                .leftOperand(leftOperand.asText())
                .operator(BinaryOperator.deserialize(operator));

        if (isValidNode(rightOperand)) {
            builder.rightOperand(new RdfResource(getOrThrow(rightOperand, "@value").asText(), URI.create(getOrThrow(rightOperand, "@type").asText())));
        } else if (isValidNode(rightOperandReference)) {
            builder.rightOperandReference(URI.create(getOrThrow(rightOperandReference, "@id").asText()));
        } else {
            throw new EdcException("Either RightOperand or RightOperandReference must be provided");
        }

        if (isValidNode(unit)) {
            builder.unit(URI.create(getOrThrow(unit, "@id").asText()));
        }
        if (isValidNode(endpoint)) {
            builder.pipEndpoint(URI.create(getOrThrow(endpoint, "@id").asText()));
        }

        return builder.build();
    }

    private static JsonNode getOrThrow(JsonNode json, String key) {
        var value = json.get(key);
        if (!isValidNode(value)) {
            throw new EdcException(String.format("Missing key: %s in %s", key, json));
        }
        return value;
    }

    private static boolean isValidNode(JsonNode node) {
        return node != null && node != NullNode.getInstance();
    }
}
