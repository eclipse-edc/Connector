/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.sql.bootstrapper;

import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapperExtension.SCHEMA_AUTOCREATE_PROPERTY;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(DependencyInjectionExtension.class)
class SqlSchemaBootstrapperExtensionTest {

    private final TransactionContext transactionContext = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TransactionContext.class, transactionContext);
    }

    @Test
    void prepare_autocreateDisabled(SqlSchemaBootstrapperExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(config.getBoolean(eq("edc.sql.schema.autocreate"), anyBoolean())).thenReturn(false);
        when(context.getConfig()).thenReturn(config);
        extension.initialize(context);
        extension.prepare();
        verifyNoInteractions(transactionContext);
    }

    @Test
    void prepare(SqlSchemaBootstrapperExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(config.getBoolean(eq(SCHEMA_AUTOCREATE_PROPERTY), anyBoolean())).thenReturn(true);
        when(context.getConfig()).thenReturn(config);
        when(transactionContext.execute(isA(TransactionContext.ResultTransactionBlock.class)))
                .thenReturn(Result.success("foobar"));

        extension.initialize(context);

        assertThatNoException().isThrownBy(extension::prepare);
        verify(transactionContext).execute(isA(TransactionContext.ResultTransactionBlock.class));
    }

}