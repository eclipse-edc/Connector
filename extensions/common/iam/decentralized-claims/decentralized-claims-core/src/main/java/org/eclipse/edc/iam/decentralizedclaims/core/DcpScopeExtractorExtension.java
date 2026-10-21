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

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.iam.decentralizedclaims.core.scope.DcpRequestScopeExtractor;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.iam.decentralizedclaims.core.DcpScopeExtractorExtension.NAME;

@Extension(NAME)
public class DcpScopeExtractorExtension implements ServiceExtension {

    public static final String NAME = "DCP scope extractor extension";

    @Inject
    private ScopeExtractorRegistry scopeExtractorRegistry;

    @Inject
    private RequestScopeExtractorRegistry requestScopeExtractorRegistry;

    @Inject
    private DcpScopeRegistry dcpScopeRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // Policy-constraint and default scopes (DcpRequestScopeExtractor) are extracted for every request message.
        requestScopeExtractorRegistry.registerExtractor(ProtocolRemoteMessage.class, new DcpRequestScopeExtractor(scopeExtractorRegistry, dcpScopeRegistry));
    }
}
