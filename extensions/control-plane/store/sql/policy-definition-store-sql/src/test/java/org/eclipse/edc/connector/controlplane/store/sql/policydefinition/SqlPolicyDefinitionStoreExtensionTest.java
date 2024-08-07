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

package org.eclipse.edc.connector.controlplane.store.sql.policydefinition;

import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.SqlPolicyDefinitionStore;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.store.sql.policydefinition.SqlPolicyStoreExtension.DATASOURCE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SqlPolicyDefinitionStoreExtensionTest {


    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void shouldInitializeTheStore(SqlPolicyStoreExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(any(), any())).thenReturn("test");

        extension.initialize(context);

        var service = context.getService(PolicyDefinitionStore.class);
        assertThat(service).isInstanceOf(SqlPolicyDefinitionStore.class);

        verify(config).getString(eq(DATASOURCE_NAME), any());
    }
}
