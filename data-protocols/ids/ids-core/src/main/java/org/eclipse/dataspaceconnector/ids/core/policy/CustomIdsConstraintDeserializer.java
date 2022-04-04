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

package org.eclipse.dataspaceconnector.ids.core.policy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.util.RdfResource;

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
        var id = jsonNode.get("@id");
        var leftOperand = jsonNode.get("ids:leftOperand");
        var operator = jsonNode.get("ids:operator");
        var rightOperand = jsonNode.get("ids:rightOperand");
        var reference = jsonNode.get("ids:rightOperandReference");
        var unit = jsonNode.get("ids:unit");
        var endpoint = jsonNode.get("ids:pipEndpoint");
        // NOTE custom properties are not deserialized

        return new IdsConstraintBuilder(URI.create(id.asText()))
                .leftOperand(leftOperand.asText())
                .operator(BinaryOperator.deserialize(operator))
                .rightOperand(new RdfResource(rightOperand.get("@value").asText(),
                        URI.create(rightOperand.get("@type").asText())))
                .rightOperandReference(URI.create(reference.asText()))
                .unit(URI.create(unit.asText()))
                .pipEndpoint(URI.create(endpoint.asText()))
                .build();
    }
}
