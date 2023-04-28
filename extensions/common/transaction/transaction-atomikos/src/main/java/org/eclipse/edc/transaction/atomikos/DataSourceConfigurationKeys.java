/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.transaction.atomikos;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

/**
 * Defines EDC data source configuration keys. All keys are prefixed by: edc.datasource.[ds name].[key]
 */
public interface DataSourceConfigurationKeys {

    @Setting(required = true, value = "The full name of the driver class")
    String DRIVER_CLASS = "driver.class";

    @Setting(required = true, value = "The URL to connect to")
    String URL = "url";

    @Setting(value = "The type of DataSource")
    String DS_TYPE = "type";

    @Setting(value = "The username used to authenticate")
    String USERNAME = "username";

    @Setting(value = "The password used to authenticate")
    String PASSWORD = "password";

    @Setting(value = "The size of the connection pool")
    String POOL_SIZE = "pool.size";

    @Setting(value = "The maximum connections in the connection pool")
    String MAX_POOL_SIZE = "max.pool.size";

    @Setting(value = "The minimum connections in the connection pool")
    String MIN_POOL_SIZE = "min.pool.size";

    @Setting(value = "The connection timeout from the pool")
    String CONNECTION_TIMEOUT = "connection.timeout";

    @Setting(value = "The login timeout response")
    String LOGIN_TIMEOUT = "login.timeout";

    @Setting(value = "The maintenance interval for the connection pool")
    String MAINTENANCE_INTERVAL = "maintenance.interval";

    @Setting(value = "The maximum idle connections that can be maintained")
    String MAX_IDLE = "max.idle";

    @Setting(value = "The time that the idle connections reaped after")
    String REAP = "reap";

    @Setting(value = "SQL query")
    String QUERY = "query";

    @Setting(value = "driver properties that are used when connecting to DB")
    String DRIVER_PROPERTIES = "properties";


}
