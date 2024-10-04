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

package org.eclipse.edc.iam.identitytrust.core;

import org.eclipse.edc.iam.identitytrust.core.scope.DcpScopeExtractorFunction;
import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.iam.identitytrust.core.DcpScopeExtractorExtension.NAME;

@Extension(NAME)
public class DcpScopeExtractorExtension implements ServiceExtension {

    public static final String NAME = "DCP scope extractor extension";

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private ScopeExtractorRegistry scopeExtractorRegistry;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        policyEngine.registerPreValidator(RequestCatalogPolicyContext.class, new DcpScopeExtractorFunction<>(scopeExtractorRegistry, monitor));
        policyEngine.registerPreValidator(RequestContractNegotiationPolicyContext.class, new DcpScopeExtractorFunction<>(scopeExtractorRegistry, monitor));
        policyEngine.registerPreValidator(RequestTransferProcessPolicyContext.class, new DcpScopeExtractorFunction<>(scopeExtractorRegistry, monitor));
    }
}
