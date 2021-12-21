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

package org.eclipse.dataspaceconnector.clients.postgresql.asset.operation;

import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.serializer.EnvelopePacker;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.PreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;

public class ContractDefinitionCreateOperation {
    private final PostgresqlClient postgresqlClient;

    public ContractDefinitionCreateOperation(@NotNull PostgresqlClient postgresClient) {
        this.postgresqlClient = Objects.requireNonNull(postgresClient);
    }

    public void invoke(@NotNull ContractDefinition contractDefinition) throws SQLException {
        Objects.requireNonNull(contractDefinition);

        String packedExpression = EnvelopePacker.pack(contractDefinition.getSelectorExpression());
        String packedAccessPolicy = EnvelopePacker.pack(contractDefinition.getAccessPolicy());
        String packedContractPolicy = EnvelopePacker.pack(contractDefinition.getContractPolicy());

        String statement = PreparedStatementResourceReader.readContractDefinitionCreate();
        postgresqlClient.doInTransaction(client -> client.execute(statement,
                contractDefinition.getId(),
                packedExpression,
                packedAccessPolicy,
                packedContractPolicy));
    }

    public void invoke(@NotNull Collection<ContractDefinition> contractDefinitions) throws SQLException {
        Objects.requireNonNull(contractDefinitions);

        String statement = PreparedStatementResourceReader.readContractDefinitionCreate();
        postgresqlClient.doInTransaction(client -> {
            for (ContractDefinition contractDefinition : contractDefinitions) {
                String packedExpression = EnvelopePacker.pack(contractDefinition.getSelectorExpression());
                String packedAccessPolicy = EnvelopePacker.pack(contractDefinition.getAccessPolicy());
                String packedContractPolicy = EnvelopePacker.pack(contractDefinition.getContractPolicy());

                client.execute(statement,
                        contractDefinition.getId(),
                        packedExpression,
                        packedAccessPolicy,
                        packedContractPolicy);
            }
        });
    }
}
