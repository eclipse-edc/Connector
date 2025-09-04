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

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SqlTransferProcessStoreExtensionTest {

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, new JacksonTypeManager());
        var provider = mock(SqlLeaseContextBuilderProvider.class);
        when(provider.createContextBuilder(any())).thenReturn(mock());
        context.registerService(SqlLeaseContextBuilderProvider.class, provider);
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
}
