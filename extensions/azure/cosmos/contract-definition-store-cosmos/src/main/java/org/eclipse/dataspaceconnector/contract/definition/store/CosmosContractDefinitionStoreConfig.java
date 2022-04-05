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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.contract.definition.store;

import org.eclipse.dataspaceconnector.azure.cosmos.AbstractCosmosConfig;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class CosmosContractDefinitionStoreConfig extends AbstractCosmosConfig {
    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    protected CosmosContractDefinitionStoreConfig(ServiceExtensionContext context) {
        super(context);
    }

    @Override
    protected String getAccountNameSetting() {
        return "edc.contractdefinitionstore.cosmos.account-name";
    }

    @Override
    protected String getDbNameSetting() {
        return "edc.contractdefinitionstore.cosmos.database-name";
    }

    @Override
    protected String getCosmosPreferredRegionSetting() {
        return "edc.contractdefinitionstore.cosmos.preferred-region";
    }

    @Override
    protected String getContainerNameSetting() {
        return "edc.contractdefinitionstore.cosmos.container-name";
    }
}
