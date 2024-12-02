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

package org.eclipse.edc.sql;

import java.sql.Connection;
import java.util.Properties;

/**
 * A ConnectionFactory combines a set of connection configuration
 * parameters that have been defined prior to connection creation.
 */
@FunctionalInterface
public interface ConnectionFactory {

    /**
     * Creates a fresh connection to the specified database
     *
     * @param jdbcUrl the JDBC url.
     * @param properties the properties.
     * @return a new Connection.
     */
    Connection create(String jdbcUrl, Properties properties);
}
