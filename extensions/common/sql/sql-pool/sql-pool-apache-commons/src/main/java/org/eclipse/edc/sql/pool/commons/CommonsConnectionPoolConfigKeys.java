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
 *       sovity GmbH - Renaming Settings to Lower Case
 *
 */

package org.eclipse.edc.sql.pool.commons;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

interface CommonsConnectionPoolConfigKeys {

    @Setting(required = false)
    String POOL_MAX_IDLE_CONNECTIONS = "pool.maxidleconnections";

    @Setting(required = false)
    String POOL_MAX_TOTAL_CONNECTIONS = "pool.maxtotalconnections";

    @Setting(required = false)
    String POOL_MIN_IDLE_CONNECTIONS = "pool.minidleconnections";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_ON_BORROW = "pool.testconnectiononborrow";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_ON_CREATE = "pool.testconnectiononcreate";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_ON_RETURN = "pool.testconnectiononreturn";

    @Setting(required = false)
    String POOL_TEST_CONNECTION_WHILE_IDLE = "pool.testconnectionwhileidle";

    @Setting(required = false)
    String POOL_TEST_QUERY = "pool.testQuery";

    @Setting(required = true)
    String URL = "url";
}
