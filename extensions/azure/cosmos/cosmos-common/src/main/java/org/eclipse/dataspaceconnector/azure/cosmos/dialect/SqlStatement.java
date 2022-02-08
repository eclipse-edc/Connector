/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Serves as entry point to creating a type-safe SQL Statement for CosmosDB.
 *
 * @param <T> The kind of {@link CosmosDocument} that will be queried
 */
public class SqlStatement<T extends CosmosDocument<?>> {

    private static final String PROPERTIES_FIELD = "wrappedInstance";
    private final Class<T> objectType;
    private final String wrapperPrefix;
    private WhereClause whereClause = new WhereClause();
    private OrderByClause orderClause = new OrderByClause();
    private LimitClause limit = new LimitClause();
    private boolean sortAscending = true;
    private OffsetClause offset = new OffsetClause();

    /**
     * Creates a new statement for a specific object type.
     *
     * @param objectType The runtime type of the object for which the statement is to be created.
     */
    public SqlStatement(Class<T> objectType) {
        this.objectType = objectType;
        wrapperPrefix = PROPERTIES_FIELD;
    }

    /**
     * Adds WHERE statements with the appropriate criteria
     *
     * @param criteria A list of criteria
     */
    public SqlStatement<T> where(List<Criterion> criteria) {
        whereClause = new WhereClause(criteria, String.join(".", objectType.getSimpleName(), wrapperPrefix));
        return this;
    }

    /**
     * Adds an ORDER BY statement
     *
     * @param orderField The field to order by. This is just the field, so no prefix, no object type. Default sort order is DESC.
     */
    public SqlStatement<T> orderBy(String orderField) {
        orderClause = new OrderByClause(orderField, sortAscending, String.join(".", objectType.getSimpleName(), wrapperPrefix));
        return this;
    }

    /**
     * Adds an ORDER BY statement
     *
     * @param orderField The field to order by. This is just the field, so no prefix, no object type.
     * @param ascending  Whether the results should be sorted in ascending or descending order.
     */
    public SqlStatement<T> orderBy(String orderField, boolean ascending) {
        sortAscending = ascending;
        return orderBy(orderField);
    }

    /**
     * Adds a LIMIT statement.
     *
     * @param limit The limit. Can be null, in which case the limit clause will be ignored. Negative limits may cause an error
     *              in the SQL database.
     */
    public SqlStatement<T> limit(@Nullable Integer limit) {
        this.limit = new LimitClause(limit);
        return this;
    }

    /**
     * Adds a OFFSET statement, effectively skipping entries.
     *
     * @param offset The offset. Can be null, in which case the offset clause will be ignored. Negative offsets
     *               may cause an error in the SQL database.
     */
    public SqlStatement<T> offset(Integer offset) {
        this.offset = new OffsetClause(offset);
        return this;
    }

    /**
     * Returns the query text. This does not expand parameterized statements.
     */
    public String getQueryAsString() {
        return getQueryAsSqlQuerySpec().getQueryText();

    }

    /**
     * Returns the parameters of the query.
     */
    public List<SqlParameter> getParameters() {
        return Stream.concat(whereClause.getParameters().stream(), orderClause.getParameters().stream())
                .collect(toList());
    }

    /**
     * Returns the entire SQL statement, potentially parameterized.
     */
    public SqlQuerySpec getQueryAsSqlQuerySpec() {
        var queryText = format("SELECT * FROM %s %s %s %s %s", objectType.getSimpleName(), whereClause.asString(), orderClause.asString(), offset.asString(), limit.asString());
        queryText = queryText.strip().replaceAll(" +", " "); //remove all unnecessary whitespaces
        var parameters = Stream.concat(whereClause.getParameters().stream(), orderClause.getParameters().stream()).collect(toList());
        return new SqlQuerySpec(queryText, parameters);
    }
}
