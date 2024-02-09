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

package org.eclipse.edc.connector.service.protocol;

import org.eclipse.edc.connector.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.RequestScope;
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

    private final Monitor monitor;

    public ProtocolTokenValidatorImpl(IdentityService identityService, PolicyEngine policyEngine, Monitor monitor) {
        this.identityService = identityService;
        this.monitor = monitor;
        this.policyEngine = policyEngine;
    }

    /**
     * Validate and extract the {@link ClaimToken} from the input {@link TokenRepresentation} by using the {@link IdentityService}
     *
     * @param tokenRepresentation The input {@link TokenRepresentation}
     * @param policyScope         The policy scope
     * @param policy              The {@link Policy}
     * @return The {@link ClaimToken} if success, failure otherwise
     */
    @Override
    public ServiceResult<ClaimToken> verifyToken(TokenRepresentation tokenRepresentation, String policyScope, Policy policy) {
        return verifyToken(tokenRepresentation, createVerificationContext(policyScope, policy));
    }

    protected ServiceResult<ClaimToken> verifyToken(TokenRepresentation tokenRepresentation, VerificationContext verificationContext) {
        var result = identityService.verifyJwtToken(tokenRepresentation, verificationContext);

        if (result.failed()) {
            monitor.debug(() -> "Unauthorized: %s".formatted(result.getFailureDetail()));
            return ServiceResult.unauthorized("Unauthorized");
        }
        return ServiceResult.success(result.getContent());
    }

    private VerificationContext createVerificationContext(String scope, Policy policy) {
        var requestScopeBuilder = RequestScope.Builder.newInstance();
        var policyContext = PolicyContextImpl.Builder.newInstance()
                .additional(RequestScope.Builder.class, requestScopeBuilder)
                .build();
        policyEngine.evaluate(scope, policy, policyContext);
        return VerificationContext.Builder.newInstance()
                .policy(policy)
                .scopes(requestScopeBuilder.build().getScopes())
                .build();
    }

}
