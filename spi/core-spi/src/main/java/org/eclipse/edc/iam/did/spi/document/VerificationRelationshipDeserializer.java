/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.did.spi.document;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializes an entry of a verification relationship (e.g. {@code assertionMethod}) of a DID document. DID Core allows
 * an entry to be either a reference to a verification method, or an embedded verification method, in which case its
 * {@code id} is returned.
 */
public class VerificationRelationshipDeserializer extends StdDeserializer<String> {

    public VerificationRelationshipDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            return parser.getString();
        }
        JsonNode node = context.readTree(parser);
        var id = node.get("id");
        if (id == null || !id.isString()) {
            return context.reportInputMismatch(this, "An embedded verification method must have a string 'id'");
        }
        return id.asString();
    }
}
