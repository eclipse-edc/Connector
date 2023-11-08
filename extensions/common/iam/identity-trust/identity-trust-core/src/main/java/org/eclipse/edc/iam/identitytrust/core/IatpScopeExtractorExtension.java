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

import org.eclipse.edc.iam.identitytrust.core.scope.IatpScopeExtractorFunction;
import org.eclipse.edc.identitytrust.scope.ScopeExtractorRegistry;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.NAME;

@Extension(NAME)
public class IatpScopeExtractorExtension implements ServiceExtension {

    public static final String NAME = "IATP scope extractor extension";

    public static final String CATALOG_REQUEST_SCOPE = "request.catalog";
    public static final String NEGOTIATION_REQUEST_SCOPE = "request.contract.negotiation";
    public static final String TRANSFER_PROCESS_REQUEST_SCOPE = "request.transfer.process";

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
        var contextMappingFunction = new IatpScopeExtractorFunction(scopeExtractorRegistry, monitor);
        policyEngine.registerPreValidator(CATALOG_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPreValidator(NEGOTIATION_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPreValidator(TRANSFER_PROCESS_REQUEST_SCOPE, contextMappingFunction);
    }
}
