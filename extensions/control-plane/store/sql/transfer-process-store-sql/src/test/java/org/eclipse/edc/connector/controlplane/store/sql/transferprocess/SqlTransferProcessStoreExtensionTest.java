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

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SqlTransferProcessStoreExtensionTest {

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void shouldInitializeTheStore(SqlTransferProcessStoreExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(any(), any())).thenReturn("test");
        
        extension.initialize(context);

        var service = context.getService(TransferProcessStore.class);
        assertThat(service).isInstanceOf(SqlTransferProcessStore.class);

    }

    @Test
    void shouldUseConfiguredLeaseDuration(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(TransferProcessStore.LEASE_DURATION_SETTING, "PT5M")));
        var extension = factory.constructInstance(SqlTransferProcessStoreExtension.class);

        extension.initialize(context);

        assertThat(context.getService(TransferProcessStore.class)).extracting("leaseDuration").isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldUseDefaultLeaseDuration_whenNotConfigured(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of()));
        var extension = factory.constructInstance(SqlTransferProcessStoreExtension.class);

        extension.initialize(context);

        assertThat(context.getService(TransferProcessStore.class)).extracting("leaseDuration").isEqualTo(Duration.ofSeconds(60));
    }
}
