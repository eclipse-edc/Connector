/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transaction.datasource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry.DEFAULT_DATASOURCE;
import static org.mockito.Mockito.mock;

class DataSourceRegistryImplTest {
    private DataSourceRegistryImpl registry;

    @Test
    void verifyRegisterAndResolve() {
        registry.register(DEFAULT_DATASOURCE, mock(DataSource.class));
        assertThat(registry.resolve(DEFAULT_DATASOURCE)).isNotNull();
        assertThat(registry.resolve("foo")).isNull();
    }

    @BeforeEach
    void setUp() {
        registry = new DataSourceRegistryImpl();
    }
}
