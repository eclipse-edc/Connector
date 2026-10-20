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

import org.eclipse.edc.iam.decentralizedclaims.core.scope.CatalogRequestScopeExtractor;
import org.eclipse.edc.iam.decentralizedclaims.core.scope.DcpRequestScopeExtractor;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractorRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DcpScopeExtractorExtensionTest {

    private final RequestScopeExtractorRegistry requestScopeExtractorRegistry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(RequestScopeExtractorRegistry.class, requestScopeExtractorRegistry);
    }

    @Test
    void initialize(ServiceExtensionContext context, DcpScopeExtractorExtension ext) {

        ext.initialize(context);

        verify(requestScopeExtractorRegistry).registerExtractor(eq(RequestCatalogPolicyContext.class), isA(DcpRequestScopeExtractor.class));
        verify(requestScopeExtractorRegistry).registerExtractor(eq(RequestCatalogPolicyContext.class), isA(CatalogRequestScopeExtractor.class));
        verify(requestScopeExtractorRegistry).registerExtractor(eq(RequestContractNegotiationPolicyContext.class), isA(DcpRequestScopeExtractor.class));
        verify(requestScopeExtractorRegistry).registerExtractor(eq(RequestTransferProcessPolicyContext.class), isA(DcpRequestScopeExtractor.class));
    }


}
