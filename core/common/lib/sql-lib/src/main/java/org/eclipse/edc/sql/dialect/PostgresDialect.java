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

package org.eclipse.edc.sql.dialect;

import static java.lang.String.format;

/**
 * Global, domain-agnostic Postgres statements, operators, functions, clauses, etc.
 */
public class PostgresDialect {
    /**
     * Creates a SELECT statement that targets a Postgres JSON array
     *
     * @param selectStatement The select statement, does not include the {@code json_array_elements} function
     *         call
     * @param jsonPath The path to the array object, which is passed as parameter to the
     *         {@code json_array_elements()} function
     * @param aliasName the alias under which the JSON array is available, e.g. for WHERE clauses
     */
    public static String getSelectFromJsonArrayTemplate(String selectStatement, String jsonPath, String aliasName) {
        return format("%s, json_array_elements(%s) as %s", selectStatement, jsonPath, aliasName);
    }

    /**
     * Returns the Postgres operator to cast a varchar to json ({@code "::json"})
     */
    public static String getJsonCastOperator() {
        return "::json";
    }

}
