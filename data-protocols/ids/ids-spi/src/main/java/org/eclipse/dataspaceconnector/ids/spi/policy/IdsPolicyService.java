/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi.policy;

import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluationResult;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

/**
 * Evaluates IDS policies.
 * Policy evaluation is performed by a provider connector when a consumer connector requests an artifact using {@link #evaluateRequest(String, String, ClaimToken, Policy)} and
 * when a consumer connector receives an offer from a provider using {@link #evaluateOffer(String, String, Policy)}.
 */
public interface IdsPolicyService {

    /**
     * Evaluates a request made by a client for an artifact.
     *
     * @param consumerConnectorId the id of the connector making the request
     * @param correlationId       an identifier that can be used to retrieve additional information about the request such as a pre-payment or pre-authorization receipt
     * @param consumerToken       the consumer's validated security token
     * @param policy              the policy attached to the artifact
     */
    PolicyEvaluationResult evaluateRequest(String consumerConnectorId, String correlationId, ClaimToken consumerToken, Policy policy);

    PolicyEvaluationResult evaluateOffer(String providerConnectorId, String processId, Policy policy);

    void registerRequestDutyFunction(String key, IdsRequestDutyFunction function);

    void registerRequestPermissionFunction(String key, IdsRequestPermissionFunction function);

    void registerRequestProhibitionFunction(String key, IdsRequestProhibitionFunction function);

    void registerOfferDutyFunction(String key, IdsOfferDutyFunction function);

    void registerOfferPermissionFunction(String key, IdsOfferPermissionFunction function);

    void registerOfferProhibitionFunction(String key, IdsOfferProhibitionFunction function);


}
