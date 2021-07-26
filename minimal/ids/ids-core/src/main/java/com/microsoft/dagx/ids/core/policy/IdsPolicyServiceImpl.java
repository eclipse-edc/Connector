/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.policy;

import com.microsoft.dagx.ids.spi.policy.IdsOfferDutyFunction;
import com.microsoft.dagx.ids.spi.policy.IdsOfferPermissionFunction;
import com.microsoft.dagx.ids.spi.policy.IdsOfferPolicyContext;
import com.microsoft.dagx.ids.spi.policy.IdsOfferProhibitionFunction;
import com.microsoft.dagx.ids.spi.policy.IdsPolicyService;
import com.microsoft.dagx.ids.spi.policy.IdsRequestDutyFunction;
import com.microsoft.dagx.ids.spi.policy.IdsRequestPermissionFunction;
import com.microsoft.dagx.ids.spi.policy.IdsRequestPolicyContext;
import com.microsoft.dagx.ids.spi.policy.IdsRequestProhibitionFunction;
import com.microsoft.dagx.policy.engine.PolicyEvaluationResult;
import com.microsoft.dagx.policy.engine.PolicyEvaluator;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.spi.iam.ClaimToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation.
 */
public class IdsPolicyServiceImpl implements IdsPolicyService {
    private Map<String, IdsRequestDutyFunction> requestDutyFunctions = new HashMap<>();
    private Map<String, IdsRequestPermissionFunction> requestPermissionFunctions = new HashMap<>();
    private Map<String, IdsRequestProhibitionFunction> requestProhibitionFunctions = new HashMap<>();

    private Map<String, IdsOfferDutyFunction> offerDutyFunctions = new HashMap<>();
    private Map<String, IdsOfferPermissionFunction> offerPermissionFunctions = new HashMap<>();
    private Map<String, IdsOfferProhibitionFunction> offerProhibitionFunctions = new HashMap<>();

    @Override
    @SuppressWarnings("DuplicatedCode")
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
    @SuppressWarnings("DuplicatedCode")
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
