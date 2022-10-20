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

package org.eclipse.edc.transaction.datasource.spi;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Default datasource registry without additional transaction manager logic
 */
public class DefaultDataSourceRegistry implements DataSourceRegistry {

    private final Map<String, DataSource> datasources = new HashMap<>();

    @Override
    public void register(String name, DataSource dataSource) {
        datasources.put(name, dataSource);
    }

    @Override
    public DataSource resolve(String name) {
        return datasources.get(name);
    }
}
