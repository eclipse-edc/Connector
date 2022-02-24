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

package org.eclipse.dataspaceconnector.sql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A ConnectionFactory combines a set of connection configuration
 * parameters that have been defined prior to connection creation.
 */
@FunctionalInterface
public interface ConnectionFactory {

    /**
     * Creates a fresh connection.
     *
     * @return connection created from a defined set of connection configuration parameters.
     */
    Connection create();
}
