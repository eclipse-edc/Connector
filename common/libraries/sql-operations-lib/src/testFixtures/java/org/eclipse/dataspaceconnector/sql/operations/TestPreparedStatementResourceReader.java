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

package org.eclipse.dataspaceconnector.sql.operations;

import org.eclipse.dataspaceconnector.sql.operations.util.PreparedStatementResourceReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

class TestPreparedStatementResourceReader {

    private static final String TABLES_CREATE = "tables_create.sql";
    private static final String TABLES_DELETE = "tables_delete.sql";

    public static String getTablesCreate() {
        return readContent(TABLES_CREATE);
    }

    public static String getTablesDelete() {
        return readContent(TABLES_DELETE);
    }

    private static String readContent(String resourceName) {
        try (InputStream inputStream = PreparedStatementResourceReader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new RuntimeException(String.format("SQL-Test: Resource not found  %s", resourceName));
            }

            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
