/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.dataplane.selector.api.v2.DataplaneSelectorApiController;
import org.eclipse.edc.connector.dataplane.selector.service.EmbeddedDataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToSelectionRequestTransformer;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSelectorApiExtensionTest {

    private final WebService webService = mock(WebService.class);
    private final ManagementApiConfiguration managementApiConfiguration = mock(ManagementApiConfiguration.class);
    private final Monitor monitor = mock(Monitor.class);
    private final TypeTransformerRegistry transformerRegistry = mock();
    private DataPlaneSelectorApiExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TypeManager.class, new TypeManager());
        context.registerService(WebService.class, webService);
        context.registerService(ManagementApiConfiguration.class, managementApiConfiguration);
        context.registerService(DataPlaneSelectorService.class, new EmbeddedDataPlaneSelectorService(
                mock(DataPlaneInstanceStore.class), mock(SelectionStrategyRegistry.class), new NoopTransactionContext()));

        TypeTransformerRegistry parentTransformerRegistry = mock();
        when(parentTransformerRegistry.forContext("management-api")).thenReturn(transformerRegistry);
        context.registerService(TypeTransformerRegistry.class, parentTransformerRegistry);
        extension = factory.constructInstance(DataPlaneSelectorApiExtension.class);
    }

    @Test
    void shouldRegisterManagementContext() {
        var config = ConfigFactory.fromMap(Collections.emptyMap());
        when(managementApiConfiguration.getContextAlias()).thenReturn("management");

        extension.initialize(contextWithConfig(config));
        verify(webService).registerResource(eq("management"), isA(DataplaneSelectorApiController.class));
        verify(transformerRegistry).register(isA(JsonObjectFromDataPlaneInstanceTransformer.class));
        verify(transformerRegistry).register(isA(JsonObjectToDataPlaneInstanceTransformer.class));
        verify(transformerRegistry).register(isA(JsonObjectToSelectionRequestTransformer.class));
    }

    @NotNull
    private DefaultServiceExtensionContext contextWithConfig(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, List.of(() -> config));
        context.initialize();
        return context;
    }
}
