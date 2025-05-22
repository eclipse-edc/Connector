/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.test.e2e.tck.presentation;

import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestVersionPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 * This extension registers a default scope mapping function, which causes a particular scope to be added to <em>every</em>
 * Presentation Query
 */
public class DefaultScopeFunctionExtension implements ServiceExtension {

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public void initialize(ServiceExtensionContext context) {

        // register a default scope provider
        var contextMappingFunction = new DefaultScopeMappingFunction(Set.of("org.eclipse.dspace.dcp.vc.type:MembershipCredential:read"));

        policyEngine.registerPostValidator(RequestCatalogPolicyContext.class, contextMappingFunction::apply);
        policyEngine.registerPostValidator(RequestContractNegotiationPolicyContext.class, contextMappingFunction::apply);
        policyEngine.registerPostValidator(RequestTransferProcessPolicyContext.class, contextMappingFunction::apply);
        policyEngine.registerPostValidator(RequestVersionPolicyContext.class, contextMappingFunction::apply);

    }
}
