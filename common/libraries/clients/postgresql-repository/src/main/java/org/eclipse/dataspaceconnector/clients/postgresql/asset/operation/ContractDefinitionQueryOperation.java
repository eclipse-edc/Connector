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
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.ContractDefinitionMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.PreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ContractDefinitionQueryOperation {
    private final PostgresqlClient postgresqlClient;

    public ContractDefinitionQueryOperation(@NotNull PostgresqlClient postgresClient) {
        this.postgresqlClient = Objects.requireNonNull(postgresClient);
    }

    @NotNull
    public List<ContractDefinition> invoke() throws SQLException {
        String statement = PreparedStatementResourceReader.readContractDefinitionSelectAll();
        return postgresqlClient.execute(new ContractDefinitionMapper(), statement);
    }
}
