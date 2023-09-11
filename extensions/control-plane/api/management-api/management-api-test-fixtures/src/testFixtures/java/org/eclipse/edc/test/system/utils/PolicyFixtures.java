/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.test.system.utils;

import jakarta.json.JsonObject;

import java.util.List;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Helper for creating Json-LD representation of Policy.
 */
public class PolicyFixtures {

    private static final String CONTRACT_EXPIRY_EVALUATION_KEY = EDC_NAMESPACE + "inForceDate";

    private PolicyFixtures() {
    }

    public static JsonObject noConstraintPolicy() {
        return createObjectBuilder()
                .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                .add(TYPE, "use")
                .build();
    }

    public static JsonObject policy(List<JsonObject> permissions) {
        return createObjectBuilder()
                .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                .add("permission", createArrayBuilder(permissions))
                .build();
    }

    public static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand) {
        return createObjectBuilder()
                .add(TYPE, "Constraint")
                .add("leftOperand", leftOperand)
                .add("operator", operator)
                .add("rightOperand", rightOperand.toString())
                .build();
    }

    public static JsonObject inForceDatePermission(String operatorStart, Object startDate, String operatorEnd, Object endDate) {
        return createObjectBuilder()
                .add("action", "use")
                .add("constraint", createObjectBuilder()
                        .add(TYPE, "LogicalConstraint")
                        .add("and", createArrayBuilder()
                                .add(atomicConstraint(CONTRACT_EXPIRY_EVALUATION_KEY, operatorStart, startDate))
                                .add(atomicConstraint(CONTRACT_EXPIRY_EVALUATION_KEY, operatorEnd, endDate))
                                .build())
                        .build())
                .build();
    }
}
