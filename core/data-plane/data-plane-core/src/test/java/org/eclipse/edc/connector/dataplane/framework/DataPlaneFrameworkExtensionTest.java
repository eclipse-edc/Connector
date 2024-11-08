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

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceRegistryImpl;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneFrameworkExtensionTest {

    private final PipelineService pipelineService = mock();

    @BeforeEach
    public void setUp(ServiceExtensionContext context) {
        context.registerService(PipelineService.class, pipelineService);
        context.registerService(ExecutorInstrumentation.class, ExecutorInstrumentation.noop());
    }

    @Test
    void initialize_registers_transferService(ServiceExtensionContext context, DataPlaneFrameworkExtension extension) {
        extension.initialize(context);

        assertThat(context.getService(TransferServiceRegistry.class)).isInstanceOf(TransferServiceRegistryImpl.class);
    }

    @Test
    void shouldClosePipelineService_whenShutdown(DataPlaneFrameworkExtension extension) {
        extension.shutdown();

        verify(pipelineService).closeAll();
    }
}
