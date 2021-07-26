/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.policy;

import com.microsoft.dagx.policy.engine.PolicyEvaluationResult;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.spi.iam.ClaimToken;

/**
 * Evaluates IDS policies.
 *
 * Policy evaluation is performed by a provider connector when a client connector requests an artifact using {@link #evaluateRequest(String, String, ClaimToken, Policy)} and
 * when a client connector receives an offer from a provider using {@link #evaluateOffer(String, String, Policy)}.
 */
public interface IdsPolicyService {

    /**
     * Evaluates a request made by a client for an an artifact.
     *
     * @param clientConnectorId the id of the connector making the request
     * @param correlationId an identifier that can be used to retrieve additional information about the request such as a pre-payment or pre-authorization receipt
     * @param clientToken the client's validated security token
     * @param policy the policy attached to the artifact
     */
    PolicyEvaluationResult evaluateRequest(String clientConnectorId, String correlationId, ClaimToken clientToken, Policy policy);

    PolicyEvaluationResult evaluateOffer(String providerConnectorId, String processId, Policy policy);

    void registerRequestDutyFunction(String key, IdsRequestDutyFunction function);

    void registerRequestPermissionFunction(String key, IdsRequestPermissionFunction function);

    void registerRequestProhibitionFunction(String key, IdsRequestProhibitionFunction function);

    void registerOfferDutyFunction(String key, IdsOfferDutyFunction function);

    void registerOfferPermissionFunction(String key, IdsOfferPermissionFunction function);

    void registerOfferProhibitionFunction(String key, IdsOfferProhibitionFunction function);


}
