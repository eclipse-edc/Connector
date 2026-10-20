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
import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractorRegistry;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Implementation of {@link ProtocolTokenValidator} which uses the {@link PolicyEngine} for extracting
 * the scope from the {@link Policy} within a scope
 */
public class ProtocolTokenValidatorImpl implements ProtocolTokenValidator {

    private final IdentityService identityService;
    private final PolicyEngine policyEngine;
    private final RequestScopeExtractorRegistry requestScopeExtractorRegistry;
    private final ParticipantAgentService agentService;
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    private final Monitor monitor;

    public ProtocolTokenValidatorImpl(IdentityService identityService, PolicyEngine policyEngine,
                                      RequestScopeExtractorRegistry requestScopeExtractorRegistry, Monitor monitor,
                                      ParticipantAgentService agentService, DataspaceProfileContextRegistry dataspaceProfileContextRegistry) {
        this.identityService = identityService;
        this.monitor = monitor;
        this.policyEngine = policyEngine;
        this.requestScopeExtractorRegistry = requestScopeExtractorRegistry;
        this.agentService = agentService;
        this.dataspaceProfileContextRegistry = dataspaceProfileContextRegistry;
    }

    @Override
    public ServiceResult<ParticipantAgent> verify(ParticipantContext participantContext, TokenRepresentation tokenRepresentation, RequestPolicyContext.Provider policyContextProvider, Policy policy, ProtocolRemoteMessage message) {
        var requestContext = RequestContext.Builder.newInstance().message(message).direction(RequestContext.Direction.Ingress).participantContextId(participantContext.getId()).build();
        var policyContext = policyContextProvider.instantiate(requestContext);
        policyEngine.evaluate(policy, policyContext);

        var profileContext = dataspaceProfileContextRegistry.getProfile(message.getProtocol());
        if (profileContext == null) {
            monitor.debug(() -> "Dataspace Profile '%s' not supported".formatted(message.getProtocol()));
            return ServiceResult.unauthorized("Unauthorized");
        }

        var scopes = requestScopeExtractorRegistry.extractScopes(policy, policyContext);
        if (scopes.failed()) {
            monitor.debug(() -> "Failed to extract scopes: %s".formatted(scopes.getFailureDetail()));
            return ServiceResult.unauthorized("Unauthorized");
        }

        var verificationContext = VerificationContext.Builder.newInstance()
                .scopes(scopes.getContent())
                .dataspaceProfileContext(profileContext)
                .build();

        var tokenValidation = identityService.verifyJwtToken(participantContext.getId(), tokenRepresentation, verificationContext);
        if (tokenValidation.failed()) {
            monitor.debug(() -> "Unauthorized: %s".formatted(tokenValidation.getFailureDetail()));
            return ServiceResult.unauthorized("Unauthorized");
        }

        var claimToken = tokenValidation.getContent();

        var id = profileContext.idExtractionFunction().apply(claimToken);
        if (id == null) {
            monitor.debug(() -> "Unauthorized: Cannot extract id on profile [%s] from claims: %s.".formatted(message.getProtocol(), claimToken.getClaims()));
            return ServiceResult.unauthorized("Unauthorized");
        }

        var participantAgent = agentService.createFor(claimToken, id);
        return ServiceResult.success(participantAgent);
    }

}
