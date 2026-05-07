/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.catalog.cache;

import org.eclipse.edc.catalog.cache.query.DspCatalogRequestAction;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class FederatedCatalogCacheExtensionTest {

    private final CrawlerActionRegistry crawlerActionRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, mock());
        context.registerService(CrawlerActionRegistry.class, crawlerActionRegistry);
    }

    @Test
    void shouldRegisterProtocolVersion(FederatedCatalogCacheExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(crawlerActionRegistry).register(eq(DATASPACE_PROTOCOL_HTTP_V_2025_1), isA(DspCatalogRequestAction.class));
    }

}
