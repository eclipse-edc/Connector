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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.ResultSetMapper;
import org.eclipse.dataspaceconnector.sql.SqlQueryExecutor;
import org.eclipse.dataspaceconnector.sql.contractdefinition.schema.SqlContractDefinitionTables;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlContractDefinitionStore implements ContractDefinitionStore {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DataSource dataSource;
    private final TransactionContext transactionContext;

    public SqlContractDefinitionStore(DataSource dataSource, TransactionContext transactionContext) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.transactionContext = Objects.requireNonNull(transactionContext);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readObject(String value) {
        try {
            Envelope envelope = OBJECT_MAPPER.readValue(value, Envelope.class);
            return (T) OBJECT_MAPPER.readValue(envelope.getContent(), Class.forName(envelope.getClassName()));
        } catch (Exception e) {
            throw new EdcException(e.getMessage(), e);
        }
    }

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        String query = "SELECT * from %s";

        try (Connection connection = dataSource.getConnection()) {
            return SqlQueryExecutor.executeQuery(
                    connection,
                    ContractDefinitionMapper.INSTANCE,
                    String.format(query, SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE));
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception);
        }
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        Objects.requireNonNull(spec);

        Limit limit = Limit.Builder.newInstance()
                .limit(spec.getLimit())
                .offset(spec.getOffset())
                .build();

        String query = "SELECT * from %s " + limit.getStatement();

        try (Connection connection = dataSource.getConnection()) {
            List<ContractDefinition> definitions = SqlQueryExecutor.executeQuery(
                    connection,
                    ContractDefinitionMapper.INSTANCE,
                    String.format(query, SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE));
            return definitions.stream();
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception);
        }
    }

    @Override
    public void save(Collection<ContractDefinition> definitions) {
        Objects.requireNonNull(definitions);

        String query = String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ACCESS_POLICY,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_CONTRACT_POLICY,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_SELECTOR);

        transactionContext.execute(() -> {
            try (Connection connection = dataSource.getConnection()) {
                definitions.forEach((definition) -> executeQuery(connection, query,
                        definition.getId(),
                        writeObject(definition.getAccessPolicy()),
                        writeObject(definition.getContractPolicy()),
                        writeObject(definition.getSelectorExpression())));
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public void save(ContractDefinition definition) {
        Objects.requireNonNull(definition);

        String query = String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ACCESS_POLICY,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_CONTRACT_POLICY,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_SELECTOR);

        transactionContext.execute(() -> {
            try (Connection connection = dataSource.getConnection()) {
                executeQuery(connection, query,
                        definition.getId(),
                        writeObject(definition.getAccessPolicy()),
                        writeObject(definition.getContractPolicy()),
                        writeObject(definition.getSelectorExpression()));
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public void update(ContractDefinition definition) {
        Objects.requireNonNull(definition);

        String query = String.format("UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ? WHERE contract_definition_id = ?",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ACCESS_POLICY,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_CONTRACT_POLICY,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_SELECTOR);


        transactionContext.execute(() -> {
            try (Connection connection = dataSource.getConnection()) {
                executeQuery(connection, query,
                        definition.getId(),
                        writeObject(definition.getAccessPolicy()),
                        writeObject(definition.getContractPolicy()),
                        writeObject(definition.getSelectorExpression()),
                        definition.getId());
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public void delete(String id) {
        Objects.requireNonNull(id);

        String query = String.format("DELETE FROM %s WHERE %s = ?",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID);

        transactionContext.execute(() -> {
            try (Connection connection = dataSource.getConnection()) {
                executeQuery(connection, query,
                        id);
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    private String writeObject(Object object) {
        try {
            String className = object.getClass().getName();
            String content = OBJECT_MAPPER.writeValueAsString(object);

            Envelope envelope = new Envelope(className, content);

            return OBJECT_MAPPER.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new EdcException(e.getMessage(), e);
        }
    }

    enum ContractDefinitionMapper implements ResultSetMapper<ContractDefinition> {
        INSTANCE;

        @Override
        public ContractDefinition mapResultSet(ResultSet resultSet) throws Exception {
            return ContractDefinition.Builder.newInstance()
                    .id(resultSet.getString(SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID))
                    .accessPolicy(readObject(resultSet.getString(SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ACCESS_POLICY)))
                    .contractPolicy(readObject(resultSet.getString(SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_CONTRACT_POLICY)))
                    .selectorExpression(readObject(resultSet.getString(SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_SELECTOR)))
                    .build();
        }
    }

    private static final class Envelope {
        private String className;
        private String content;

        public Envelope() {
        }

        public Envelope(String className, String content) {
            this.className = className;
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }
    }

}