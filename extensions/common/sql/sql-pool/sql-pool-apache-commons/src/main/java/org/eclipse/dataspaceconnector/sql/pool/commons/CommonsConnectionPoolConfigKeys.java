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

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;

interface CommonsConnectionPoolConfigKeys {

    @Setting(required = false)
    String POOL_MAX_IDLE_CONNECTIONS = "pool.maxIdleConnections";

    @Setting(required = false)
    String POOL_MAX_TOTAL_CONNECTIONS = "pool.maxTotalConnections";

    @Setting(required = false)
    String POOL_MIN_IDLE_CONNECTIONS = "pool.minIdleConnections";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_ON_BORROW = "pool.testConnectionOnBorrow";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_ON_CREATE = "pool.testConnectionOnCreate";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_ON_RETURN = "pool.testConnectionOnReturn";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_WHILE_IDLE = "pool.testConnectionWhileIdle";

    @Setting(required = false)
    String POOL_TEST_QUERY = "pool.testQuery";

    @Setting(required = true)
    String URL = "url";
}
