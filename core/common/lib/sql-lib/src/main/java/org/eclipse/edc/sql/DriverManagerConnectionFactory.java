/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.edc.sql;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DriverManagerConnectionFactory implements ConnectionFactory {

    @Override
    public Connection create(String jdbcUrl, Properties properties) {
        try {
            return DriverManager.getConnection(jdbcUrl, properties);
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception.getMessage(), exception);
        }
    }

}
