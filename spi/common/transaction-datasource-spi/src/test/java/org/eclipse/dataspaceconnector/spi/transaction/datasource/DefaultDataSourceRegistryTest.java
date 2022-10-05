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

package org.eclipse.dataspaceconnector.spi.transaction.datasource;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultDataSourceRegistryTest {


    @Test
    void verifyRegisterAndResolve() {
        var registry = new DefaultDataSourceRegistry();
        var datasource = mock(DataSource.class);

        registry.register(DataSourceRegistry.DEFAULT_DATASOURCE, datasource);
        assertThat(registry.resolve(DataSourceRegistry.DEFAULT_DATASOURCE)).isNotNull().isEqualTo(datasource);
        assertThat(registry.resolve("foo")).isNull();

    }
}
