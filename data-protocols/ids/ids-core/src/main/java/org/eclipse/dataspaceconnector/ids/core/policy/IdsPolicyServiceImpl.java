/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.ids.core.policy;

import org.eclipse.dataspaceconnector.ids.spi.policy.IdsOfferDutyFunction;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsOfferPermissionFunction;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsOfferPolicyContext;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsOfferProhibitionFunction;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsRequestDutyFunction;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsRequestPermissionFunction;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsRequestPolicyContext;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsRequestProhibitionFunction;
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
