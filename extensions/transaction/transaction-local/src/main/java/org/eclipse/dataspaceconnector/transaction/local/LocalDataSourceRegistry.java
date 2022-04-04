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

package org.eclipse.dataspaceconnector.transaction.local;

import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.transaction.local.LocalTransactionContextManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Default {@link DataSourceRegistry} implementation. This implementation is used to register both local and XA data sources so that they can be resolved by extensions.
 */
public class LocalDataSourceRegistry implements DataSourceRegistry {
    private LocalTransactionContextManager manager;

    private Map<String, DataSource> dataSources = new HashMap<>();

    public LocalDataSourceRegistry(LocalTransactionContextManager manager) {
        this.manager = manager;
    }

    @Override
    public void register(String name, DataSource dataSource) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dataSource, "dataSource");
        var wrapper = new DataSourceResource(dataSource);
        dataSources.put(name, wrapper);
        manager.registerResource(wrapper);
    }

    @Override
    public DataSource resolve(String name) {
        return dataSources.get(name);
    }
}
