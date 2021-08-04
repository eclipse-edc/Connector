/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.policy;

import org.eclipse.dataspaceconnector.ids.spi.policy.*;
import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluationResult;
import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluator;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation.
 */
public class IdsPolicyServiceImpl implements IdsPolicyService {
    private final Map<String, IdsRequestDutyFunction> requestDutyFunctions = new HashMap<>();
    private final Map<String, IdsRequestPermissionFunction> requestPermissionFunctions = new HashMap<>();
    private final Map<String, IdsRequestProhibitionFunction> requestProhibitionFunctions = new HashMap<>();

    private final Map<String, IdsOfferDutyFunction> offerDutyFunctions = new HashMap<>();
    private final Map<String, IdsOfferPermissionFunction> offerPermissionFunctions = new HashMap<>();
    private final Map<String, IdsOfferProhibitionFunction> offerProhibitionFunctions = new HashMap<>();

    @Override
    public PolicyEvaluationResult evaluateRequest(String clientConnectorId, String correlationId, ClaimToken clientToken, Policy policy) {
        var context = new IdsRequestPolicyContext(clientConnectorId, correlationId, clientToken);

        var evalBuilder = PolicyEvaluator.Builder.newInstance();

        requestDutyFunctions.forEach((key, fn) -> evalBuilder.dutyFunction(key, (operator, value, duty) -> fn.evaluate(operator, (String) value, duty, context)));
        requestPermissionFunctions.forEach((key, fn) -> evalBuilder.permissionFunction(key, (operator, value, duty) -> fn.evaluate(operator, (String) value, duty, context)));
        requestProhibitionFunctions.forEach((key, fn) -> evalBuilder.prohibitionFunction(key, (operator, value, duty) -> fn.evaluate(operator, (String) value, duty, context)));

        PolicyEvaluator evaluator = evalBuilder.build();
        return evaluator.evaluate(policy);
    }

    @Override
    public PolicyEvaluationResult evaluateOffer(String providerConnectorId, String processId, Policy policy) {
        var context = new IdsOfferPolicyContext(providerConnectorId, processId);

        var evalBuilder = PolicyEvaluator.Builder.newInstance();

        offerDutyFunctions.forEach((key, fn) -> evalBuilder.dutyFunction(key, (operator, value, duty) -> fn.evaluate(operator, (String) value, duty, context)));
        offerPermissionFunctions.forEach((key, fn) -> evalBuilder.permissionFunction(key, (operator, value, duty) -> fn.evaluate(operator, (String) value, duty, context)));
        offerProhibitionFunctions.forEach((key, fn) -> evalBuilder.prohibitionFunction(key, (operator, value, duty) -> fn.evaluate(operator, (String) value, duty, context)));

        PolicyEvaluator evaluator = evalBuilder.build();
        return evaluator.evaluate(policy);
    }

    @Override
    public void registerRequestDutyFunction(String key, IdsRequestDutyFunction function) {
        requestDutyFunctions.put(key, function);
    }

    @Override
    public void registerRequestPermissionFunction(String key, IdsRequestPermissionFunction function) {
        requestPermissionFunctions.put(key, function);
    }

    @Override
    public void registerRequestProhibitionFunction(String key, IdsRequestProhibitionFunction function) {
        requestProhibitionFunctions.put(key, function);
    }

    @Override
    public void registerOfferDutyFunction(String key, IdsOfferDutyFunction function) {
        offerDutyFunctions.put(key, function);
    }

    @Override
    public void registerOfferPermissionFunction(String key, IdsOfferPermissionFunction function) {
        offerPermissionFunctions.put(key, function);
    }

    @Override
    public void registerOfferProhibitionFunction(String key, IdsOfferProhibitionFunction function) {
        offerProhibitionFunctions.put(key, function);
    }

}
