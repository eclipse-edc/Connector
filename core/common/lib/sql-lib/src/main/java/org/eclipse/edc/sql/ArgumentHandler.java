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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Component capable of setting a certain parameter type to it's
 * corresponding position within a {@link java.sql.PreparedStatement}
 */
interface ArgumentHandler {

    /**
     * Tests whether an argument can be used by the current handler
     *
     * @param value to be associated with the prepared statement
     * @return true if the current argument handler can act on the given argument
     */
    boolean accepts(Object value);

    /**
     * Associates an argument with a given SQL statement at its specific position
     *
     * @param statement to be carrying the argument
     * @param position  to be used for carrying the argument
     * @param argument  to be used together with the statement
     * @throws SQLException if something went wrong
     */
    void handle(PreparedStatement statement, int position, Object argument) throws SQLException;
}
