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
package org.eclipse.dataspaceconnector.spi.transaction.datasource;

import javax.sql.DataSource;

/**
 * A registry of configured data sources.
 */
public interface DataSourceRegistry {

    /**
     * The default data source.
     */
    String DEFAULT_DATASOURCE = "default";

    /**
     * Registers a datasource.
     */
    void register(String name, DataSource dataSource);

    /**
     * Returns the datasource registered for the name or null if not found.
     */
    DataSource resolve(String name);

}
