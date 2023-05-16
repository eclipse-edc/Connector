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

    @Setting(required = true)
    String DRIVER_CLASS = "driver.class";

    @Setting(required = true)
    String URL = "url";

    @Setting
    String DS_TYPE = "type";

    @Setting
    String USERNAME = "username";

    @Setting
    String PASSWORD = "password";

    @Setting
    String POOL_SIZE = "pool.size";

    @Setting
    String MAX_POOL_SIZE = "max.pool.size";

    @Setting
    String MIN_POOL_SIZE = "min.pool.size";

    @Setting
    String CONNECTION_TIMEOUT = "connection.timeout";

    @Setting
    String LOGIN_TIMEOUT = "login.timeout";

    @Setting
    String MAINTENANCE_INTERVAL = "maintenance.interval";

    @Setting
    String MAX_IDLE = "max.idle";

    @Setting
    String QUERY = "query";

    @Setting
    String DRIVER_PROPERTIES = "properties";


}
