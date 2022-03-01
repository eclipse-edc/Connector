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

package org.eclipse.dataspaceconnector.sql.contractdefinition.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.contractdefinition.schema.SqlContractDefinitionTables;

import java.sql.Connection;
import java.util.Objects;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlContractDefinitionLoader implements ContractDefinitionLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DataSource dataSource;
    private final TransactionContext transactionContext;

    public SqlContractDefinitionLoader(DataSource dataSource, TransactionContext transactionContext) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.transactionContext = Objects.requireNonNull(transactionContext);
    }

    @Override
    public void accept(ContractDefinition definition) {
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

    @SuppressWarnings("unchecked")
    private <T> T readObject(String value) {
        try {
            Envelope envelope = OBJECT_MAPPER.readValue(value, Envelope.class);
            return (T) OBJECT_MAPPER.readValue(envelope.getContent(), Class.forName(envelope.getClassName()));
        } catch (Exception e) {
            throw new EdcException(e.getMessage(), e);
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
