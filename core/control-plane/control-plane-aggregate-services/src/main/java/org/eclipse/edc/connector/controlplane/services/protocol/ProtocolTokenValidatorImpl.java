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
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.controlplane.services.protocol;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Implementation of {@link ProtocolTokenValidator} which uses the {@link PolicyEngine} for extracting
 * the scope from the {@link Policy} within a scope
 */
public class ProtocolTokenValidatorImpl implements ProtocolTokenValidator {

    private final IdentityService identityService;
    private final PolicyEngine policyEngine;
    private final ParticipantAgentService agentService;
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    private final Monitor monitor;

    public ProtocolTokenValidatorImpl(IdentityService identityService, PolicyEngine policyEngine, Monitor monitor,
                                      ParticipantAgentService agentService, DataspaceProfileContextRegistry dataspaceProfileContextRegistry) {
        this.identityService = identityService;
        this.monitor = monitor;
        this.policyEngine = policyEngine;
        this.agentService = agentService;
        this.dataspaceProfileContextRegistry = dataspaceProfileContextRegistry;
    }

    @Override
    public ServiceResult<ParticipantAgent> verify(ParticipantContext participantContext, TokenRepresentation tokenRepresentation, RequestPolicyContext.Provider policyContextProvider, Policy policy, RemoteMessage message) {
        var requestScopeBuilder = RequestScope.Builder.newInstance();
        var requestContext = RequestContext.Builder.newInstance().message(message).direction(RequestContext.Direction.Ingress).build();
        var policyContext = policyContextProvider.instantiate(requestContext, requestScopeBuilder);
        policyEngine.evaluate(policy, policyContext);
        var verificationContext = VerificationContext.Builder.newInstance()
                .policy(policy)
                .scopes(policyContext.requestScopeBuilder().build().getScopes())
                .build();
        var tokenValidation = identityService.verifyJwtToken(tokenRepresentation, verificationContext);
        if (tokenValidation.failed()) {
            monitor.debug(() -> "Unauthorized: %s".formatted(tokenValidation.getFailureDetail()));
            return ServiceResult.unauthorized("Unauthorized");
        }

        var claimToken = tokenValidation.getContent();

        var idExtractionFunction = dataspaceProfileContextRegistry.getIdExtractionFunction(message.getProtocol());
        if (idExtractionFunction == null) {
            return ServiceResult.badRequest("Unsupported protocol: " + message.getProtocol());
        }

        var participantAgent = agentService.createFor(claimToken, idExtractionFunction.apply(claimToken));
        return ServiceResult.success(participantAgent);
    }

}
