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

package org.eclipse.edc.edr.store.index.sql;

import org.eclipse.edc.edr.store.index.SqlJtiValidationStore;
import org.eclipse.edc.edr.store.index.SqlJtiValidationStoreExtension;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.edr.store.index.SqlJtiValidationStoreExtension.DATASOURCE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SqlJtiValidationStoreExtensionTest {

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void shouldInitializeTheStore(SqlJtiValidationStoreExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(any(), any())).thenReturn("test");

        extension.initialize(context);

        var service = context.getService(JtiValidationStore.class);
        assertThat(service).isInstanceOf(SqlJtiValidationStore.class);

        verify(config).getString(eq(DATASOURCE_NAME), any());
    }

    @Test
    void shouldStartReaperThread_withDefaultConfig(SqlJtiValidationStoreExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getLong(SqlJtiValidationStoreExtension.CLEANUP_PERIOD, any())).thenReturn(1L);

        extension.initialize(context);
        extension.start();
    }
}
