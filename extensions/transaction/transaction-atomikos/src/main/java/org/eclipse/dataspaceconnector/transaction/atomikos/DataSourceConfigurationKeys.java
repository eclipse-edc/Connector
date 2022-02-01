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
package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

/**
 * Defines EDC data source configuration keys. All keys are prefixed by: edc.datasource.[ds name].[key]
 */
public interface DataSourceConfigurationKeys {

    @EdcSetting(required = true)
    String DRIVER_CLASS = "driver.class";

    @EdcSetting(required = true)
    String URL = "url";

    @EdcSetting
    String DS_TYPE = "type";

    @EdcSetting
    String USERNAME = "username";

    @EdcSetting
    String PASSWORD = "password";

    @EdcSetting
    String POOL_SIZE = "pool.size";

    @EdcSetting
    String MAX_POOL_SIZE = "max.pool.size";

    @EdcSetting
    String MIN_POOL_SIZE = "min.pool.size";

    @EdcSetting
    String CONNECTION_TIMEOUT = "connection.timeout";

    @EdcSetting
    String LOGIN_TIMEOUT = "login.timeout";

    @EdcSetting
    String MAINTENANCE_INTERVAL = "maintenance.interval";

    @EdcSetting
    String MAX_IDLE = "max.idle";

    @EdcSetting
    String REAP = "reap";

    @EdcSetting
    String QUERY = "query";

    @EdcSetting
    String DRIVER_PROPERTIES = "properties";


}
