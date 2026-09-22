/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.partner;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class SqlPartnerStoreExtensionTest {

    private final SqlSchemaBootstrapper bootstrapper = mock();
    private final TypeManager typeManager = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(SqlSchemaBootstrapper.class, bootstrapper);
        context.registerService(TypeManager.class, typeManager);
        when(typeManager.getMapper()).thenReturn(mock());
    }

    @Test
    void shouldProvideStoresAndRegisterSchema(SqlPartnerStoreExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        assertThat(extension.partnerStore()).isInstanceOf(SqlPartnerStore.class);
        assertThat(extension.partnerGroupStore()).isInstanceOf(SqlPartnerGroupStore.class);
        verify(bootstrapper).addStatementFromResource(any(), eq("partner-schema.sql"));
    }
}
