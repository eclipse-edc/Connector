/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BootstrapTest {
    private ObjectMapper mapper;

    @Test
    public void catalog() throws Exception {
        var json = mapper.readValue(getClass().getClassLoader().getResource("catalog.message.json"), JsonObject.class);

        var expanded = JsonLd.expand(JsonDocument.of(json)).get();
        var tree = mapper.convertValue(mapper.readValue(getClass().getClassLoader().getResource("catalog.message.json"), Object.class), JsonObject.class);
        // mapper.convertValue()
        var compacted = JsonLd.compact(JsonDocument.of(expanded), JsonDocument.of(json)).get();
        var flattened = JsonLd.flatten(JsonDocument.of(json)).get();
    }

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JSONPModule());
        var module = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
            }
        };
        mapper.registerModule(module);
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);
    }

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var action = Action.Builder.newInstance().type("USE").build();

        var constraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("left"))
                .rightExpression(new LiteralExpression("right"))
                .build();

        var permission = Permission.Builder.newInstance().action(action).constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var serialized = mapper.writeValueAsString(policy);
        mapper.readValue(serialized, Policy.class).getPermissions();
    }

}


