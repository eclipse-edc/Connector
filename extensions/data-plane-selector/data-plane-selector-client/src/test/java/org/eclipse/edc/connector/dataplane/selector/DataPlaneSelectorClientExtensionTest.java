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

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSelectorClientExtensionTest {

    private final TypeTransformerRegistry typeTransformerRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.dataplane.client.selector.strategy", "http://any",
                "edc.dpf.selector.url", "http://any")));

    }

    @Test
    void dataPlaneSelectorService_shouldReturnRemoteService(DataPlaneSelectorClientExtension extension, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of()));

        var client = extension.dataPlaneSelectorService(context);

        assertThat(client).isInstanceOf(RemoteDataPlaneSelectorService.class);
    }

    @Test
    void dataPlaneSelectorService_shouldThrowException_whenUrlNotConfigured(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(emptyMap()));

        assertThatThrownBy(() -> objectFactory.constructInstance(DataPlaneSelectorClientExtension.class).dataPlaneSelectorService(context)).isInstanceOf(EdcException.class)
                .hasMessageContaining("No config value and no default value found for injected field");
    }

    @Test
    void initialize_shouldRegisterTransformer(DataPlaneSelectorClientExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(typeTransformerRegistry).register(isA(JsonObjectFromDataPlaneInstanceTransformer.class));
    }
}
