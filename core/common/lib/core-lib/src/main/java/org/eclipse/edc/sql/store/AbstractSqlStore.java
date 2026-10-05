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

package org.eclipse.edc.sql.store;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.sql.DataSource;

import static java.lang.String.format;

public abstract class AbstractSqlStore {
    protected final TransactionContext transactionContext;
    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    protected final QueryExecutor queryExecutor;
    private final Supplier<ObjectMapper> objectMapperSupplier;

    public AbstractSqlStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                            Supplier<ObjectMapper> objectMapperSupplier, QueryExecutor queryExecutor) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.objectMapperSupplier = objectMapperSupplier;
        this.queryExecutor = queryExecutor;
    }

    @Deprecated(since = "1.0.0")
    public AbstractSqlStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper, QueryExecutor queryExecutor) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.objectMapperSupplier = () -> objectMapper;
        this.queryExecutor = queryExecutor;
    }

    protected Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    protected String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return object instanceof String ? object.toString() : objectMapperSupplier.get().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new EdcPersistenceException(e);
        }
    }

    protected <T> String toJson(Object object, TypeReference<T> typeReference) {
        if (object == null) {
            return null;
        }
        try {
            return object instanceof String ? object.toString() : objectMapperSupplier.get().writerFor(typeReference).writeValueAsString(object);
        } catch (JacksonException e) {
            throw new EdcPersistenceException(e);
        }
    }

    protected <T> T fromJson(String json, TypeReference<T> typeReference) {
        return fromJson(json, objectMapperSupplier.get().constructType(typeReference));
    }

    protected <T> T fromJson(String json, Class<T> type) {
        return fromJson(json, objectMapperSupplier.get().constructType(type));
    }

    protected <T> T fromJson(String json, JavaType type) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapperSupplier.get().readValue(json, type);
        } catch (JacksonException e) {
            throw new EdcPersistenceException(e);
        }
    }

    protected <T> CollectionType listOf(Class<T> clazz) {
        return objectMapperSupplier.get().getTypeFactory().constructCollectionType(List.class, clazz);
    }

    @NotNull
    protected <T> TypeReference<T> getTypeRef() {
        return new TypeReference<>() {
        };
    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), format("DataSource %s could not be resolved", dataSourceName));
    }

}
