/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

/**
 * Configuration class for {@link SqlQueryExecutor}
 */
public record SqlQueryExecutorConfiguration(int fetchSize) {

    public static final String DEFAULT_EDC_SQL_FETCH_SIZE = "5000";
    @Setting(value = "Fetch size value used in SQL queries", defaultValue = DEFAULT_EDC_SQL_FETCH_SIZE)
    public static final String EDC_SQL_FETCH_SIZE = "edc.sql.fetch.size";

    public static SqlQueryExecutorConfiguration ofDefaults() {
        return new SqlQueryExecutorConfiguration(Integer.parseInt(DEFAULT_EDC_SQL_FETCH_SIZE));
    }

}
