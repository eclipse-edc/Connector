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

package org.eclipse.edc.sql.pool.commons;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

import java.util.Map;

interface CommonsConnectionPoolConfigKeys {

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_MAX_IDLE_CONNECTIONS = "pool.maxIdleConnections";
    String POOL_CONNECTIONS_MAX_IDLE = "pool.connections.max-idle";

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_MAX_TOTAL_CONNECTIONS = "pool.maxTotalConnections";

    String POOL_CONNECTIONS_MAX_TOTAL = "pool.connections.max-total";

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_MIN_IDLE_CONNECTIONS = "pool.minIdleConnections";
    String POOL_CONNECTIONS_MIN_IDLE = "pool.connections.min-idle";

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_TEST_CONNECTION_ON_BORROW = "pool.testConnectionOnBorrow";
    String POOL_CONNECTION_TEST_ON_BORROW = "pool.connection.test.on-borrow";

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_TEST_CONNECTION_ON_CREATE = "pool.testConnectionOnCreate";

    String POOL_CONNECTION_TEST_ON_CREATE = "pool.connection.test.on-create";

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_TEST_CONNECTION_ON_RETURN = "pool.testConnectionOnReturn";

    String POOL_CONNECTION_TEST_ON_RETURN = "pool.connection.test.on-return";

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_TEST_CONNECTION_WHILE_IDLE = "pool.testConnectionWhileIdle";
    String POOL_CONNECTION_TEST_WHILE_IDLE = "pool.connection.test.while-idle";

    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    String DEPRACATED_POOL_TEST_QUERY = "pool.testQuery";

    String POOL_CONNECTION_TEST_QUERY = "pool.connection.test.query";

    @Setting(required = true)
    String URL = "url";

    Map<String, String> CONFIGURATION_MAPPING = Map.of(
            POOL_CONNECTIONS_MAX_IDLE, DEPRACATED_POOL_MAX_IDLE_CONNECTIONS,
            POOL_CONNECTIONS_MIN_IDLE, DEPRACATED_POOL_MIN_IDLE_CONNECTIONS,
            POOL_CONNECTIONS_MAX_TOTAL, DEPRACATED_POOL_MAX_TOTAL_CONNECTIONS,
            POOL_CONNECTION_TEST_ON_BORROW, DEPRACATED_POOL_TEST_CONNECTION_ON_BORROW,
            POOL_CONNECTION_TEST_ON_CREATE, DEPRACATED_POOL_TEST_CONNECTION_ON_CREATE,
            POOL_CONNECTION_TEST_ON_RETURN, DEPRACATED_POOL_TEST_CONNECTION_ON_RETURN,
            POOL_CONNECTION_TEST_WHILE_IDLE, DEPRACATED_POOL_TEST_CONNECTION_WHILE_IDLE,
            POOL_CONNECTION_TEST_QUERY, DEPRACATED_POOL_TEST_QUERY);

}
