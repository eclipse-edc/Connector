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

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapperExtension.SCHEMA_AUTOCREATE_PROPERTY;
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
    void shouldNotCreateSchema_whenSettingIsDisabled(ObjectFactory objectFactory, ServiceExtensionContext context) {
        var extension = objectFactory.constructInstance(SqlSchemaBootstrapperExtension.class);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(SCHEMA_AUTOCREATE_PROPERTY, "false")));

        extension.initialize(context);
        extension.prepare();

        verifyNoInteractions(transactionContext);
    }

    @Test
    void shouldCreateSchema_whenSettingIsEnabled(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(SCHEMA_AUTOCREATE_PROPERTY, "true")));
        var extension = objectFactory.constructInstance(SqlSchemaBootstrapperExtension.class);
        when(transactionContext.execute(isA(TransactionContext.ResultTransactionBlock.class)))
                .thenReturn(Result.success("foobar"));

        extension.initialize(context);

        assertThatNoException().isThrownBy(extension::prepare);
        verify(transactionContext).execute(isA(TransactionContext.ResultTransactionBlock.class));
    }

}
