/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.datspaceconnector.sql.assetindex;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.sql.assetindex.SqlAssetIndexServiceExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

public class SqlAssetIndexServiceExtensionTest {

    private static final String DATA_SOURCE_NAME = "test-source";

    // mocks
    private ServiceExtensionContext context;
    private DataSourceRegistry registry;

    @BeforeEach
    public void setup() {
        context = Mockito.mock(ServiceExtensionContext.class);
        registry = Mockito.mock(DataSourceRegistry.class);
        DataSource dataSource = Mockito.mock(DataSource.class);

        Mockito.when(context.getService(DataSourceRegistry.class)).thenReturn(registry);
        Mockito.when(context.getSetting(SqlAssetIndexServiceExtension.DATA_SOURCE_NAME_SETTING, DataSourceRegistry.DEFAULT_DATASOURCE))
                .thenReturn(DATA_SOURCE_NAME);
        Mockito.when(registry.resolve(DATA_SOURCE_NAME)).thenReturn(dataSource);
    }

    @Test
    public void testNoExceptionOnDataSourceRegistered() {
        SqlAssetIndexServiceExtension serviceExtension = new SqlAssetIndexServiceExtension();

        Assertions.assertDoesNotThrow(() -> serviceExtension.initialize(context));
    }

    @Test
    public void testExceptionOnDataSourceNotRegistered() {
        Mockito.when(registry.resolve(DATA_SOURCE_NAME)).thenReturn(null);

        SqlAssetIndexServiceExtension serviceExtension = new SqlAssetIndexServiceExtension();

        Assertions.assertThrows(EdcException.class, () -> serviceExtension.initialize(context));
    }
}
