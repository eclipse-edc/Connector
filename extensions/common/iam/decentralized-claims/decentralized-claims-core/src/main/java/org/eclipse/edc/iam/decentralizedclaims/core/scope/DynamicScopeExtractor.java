/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core.scope;

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts scopes dynamically from the DCP scope registry based on the request context.
 */
public class DynamicScopeExtractor implements ScopeExtractor {
    private static final Set<Class<? extends RemoteMessage>> SUPPORTED_MESSAGES = Set.of(CatalogRequestMessage.class, ContractRequestMessage.class, TransferRequestMessage.class);
    private final DcpScopeRegistry registry;

    public DynamicScopeExtractor(DcpScopeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Set<String> extractScopes(Object leftValue, Operator operator, Object rightValue, RequestPolicyContext context) {
        // extract only for supported messages
        if (!SUPPORTED_MESSAGES.contains(context.requestContext().getMessage().getClass())) {
            return Set.of();
        }
        var result = registry.getScopeMapping();
        if (result.failed()) {
            context.reportProblem("Failed to get scope mapping: " + result.getFailureMessages());
            return Set.of();
        }
        return result.getContent().stream().filter(scope -> filterScope(scope, leftValue, context))
                .map(DcpScope::getValue)
                .collect(Collectors.toSet());

    }

    private boolean filterScope(DcpScope scope, Object leftValue, RequestPolicyContext context) {
        if (leftValue instanceof String leftOperand) {
            return (leftOperand.startsWith(scope.getPrefixMapping())) &&
                    (scope.getProfile().equals(DcpScope.WILDCARD) || scope.getProfile().equals(context.requestContext().getMessage().getProtocol()));
        } else {
            return false;
        }
    }
}
