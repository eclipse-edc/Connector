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

package org.eclipse.dataspaceconnector.sql.pool.commons;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

interface CommonsConnectionPoolConfigKeys {

    @EdcSetting(required = false)
    String POOL_MAX_IDLE_CONNECTIONS = "pool.maxIdleConnections";

    @EdcSetting(required = false)
    String POOL_MAX_TOTAL_CONNECTIONS = "pool.maxTotalConnections";

    @EdcSetting(required = false)
    String POOL_MIN_IDLE_CONNECTIONS = "pool.minIdleConnections";

    @EdcSetting(required = false)
    String POOL_TEST_CONNECTION_ON_BORROW = "pool.testConnectionOnBorrow";

    @EdcSetting(required = false)
    String POOL_TEST_CONNECTION_ON_CREATE = "pool.testConnectionOnCreate";

    @EdcSetting(required = false)
    String POOL_TEST_CONNECTION_ON_RETURN = "pool.testConnectionOnReturn";

    @EdcSetting(required = false)
    String POOL_TEST_CONNECTION_WHILE_IDLE = "pool.testConnectionWhileIdle";

    @EdcSetting(required = false)
    String POOL_TEST_QUERY = "pool.testQuery";

    @EdcSetting(required = true)
    String URL = "url";
}
