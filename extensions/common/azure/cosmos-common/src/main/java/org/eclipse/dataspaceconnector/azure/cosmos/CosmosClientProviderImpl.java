/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.cosmos;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.Vault;

import java.util.List;

class CosmosClientProviderImpl implements CosmosClientProvider {

    private static final String HOST_TEMPLATE = "https://%s.documents.azure.com:443/";

    @Override
    public CosmosClient createClient(Vault vault, AbstractCosmosConfig configuration) {
        var accountName = configuration.getAccountName();
        var accountKey = vault.resolveSecret(accountName);
        if (StringUtils.isNullOrEmpty(accountKey)) {
            throw new EdcException("No credentials found in vault for Cosmos DB '" + accountName + "'");
        }

        var host = String.format(HOST_TEMPLATE, accountName);
        return new CosmosClientBuilder()
                .endpoint(host)
                .key(accountKey)
                .preferredRegions(List.of(configuration.getPreferredRegion()))
                .consistencyLevel(ConsistencyLevel.SESSION)
                .buildClient();
    }
}
