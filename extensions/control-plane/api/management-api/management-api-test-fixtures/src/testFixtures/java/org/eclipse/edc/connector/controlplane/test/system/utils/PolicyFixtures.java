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

package org.eclipse.edc.connector.controlplane.test.system.utils;

import jakarta.json.JsonObject;

import java.util.List;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Helper for creating Json-LD representation of Policy.
 */
public class PolicyFixtures {

    private static final String CONTRACT_EXPIRY_EVALUATION_KEY = EDC_NAMESPACE + "inForceDate";
    private static final String ODRL_JSONLD = "http://www.w3.org/ns/odrl.jsonld";

    private PolicyFixtures() {
    }

    public static JsonObject noConstraintPolicy() {
        return createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .build();
    }

    public static JsonObject policy(List<JsonObject> permissions) {
        return createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .add("permission", createArrayBuilder(permissions))
                .build();
    }

    /**
     * Create an ODRL policy that forces the contract to expire after the offset passed.
     *
     * @param offset the offset, examples of working ones are 10s (10 seconds), 2m (2 minutes), 1h (1 hour)
     * @return the policy.
     */
    public static JsonObject contractExpiresIn(String offset) {
        return inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+" + offset);
    }

    /**
     * Create a policy with "inForceDate" permissions, to enforce contract date being in a certain range.
     * Please check the ContractExpiryCheckFunction documentation for details.
     *
     * @param operatorStart the operator used for the start date.
     * @param startDate the start date.
     * @param operatorEnd the operator used for the end date.
     * @param endDate the end date.
     * @return the policy json-ld representation object.
     */
    public static JsonObject inForceDatePolicy(String operatorStart, Object startDate, String operatorEnd, Object endDate) {
        return policy(List.of(inForceDatePermission(operatorStart, startDate, operatorEnd, endDate)));
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
