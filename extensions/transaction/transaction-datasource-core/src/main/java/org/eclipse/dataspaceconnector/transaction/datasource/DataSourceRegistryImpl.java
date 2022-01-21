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

import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Default {@link DataSourceRegistry} implementation. This implementation is used to register both local and XA data sources so that they can be resolved by extensions.
 */
public class DataSourceRegistryImpl implements DataSourceRegistry {
    private Map<String, DataSource> dataSources = new HashMap<>();

    @Override
    public void register(String name, DataSource dataSource) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dataSource, "dataSource");
        dataSources.put(name, dataSource);
    }

    @Override
    public @Nullable DataSource resolve(String name) {
        return dataSources.get(name);
    }
}
