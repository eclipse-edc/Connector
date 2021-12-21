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

package org.eclipse.dataspaceconnector.postgresql.contractdefinitionloader;

import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Objects;

public class PostgresContractDefinitionLoader implements ContractDefinitionLoader {

    private final Repository assetRepository;

    public PostgresContractDefinitionLoader(@NotNull Repository assetRepository) {
        this.assetRepository = Objects.requireNonNull(assetRepository);
    }

    @Override
    public void accept(ContractDefinition item) {
        try {
            assetRepository.create(Objects.requireNonNull(item));
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }
}
