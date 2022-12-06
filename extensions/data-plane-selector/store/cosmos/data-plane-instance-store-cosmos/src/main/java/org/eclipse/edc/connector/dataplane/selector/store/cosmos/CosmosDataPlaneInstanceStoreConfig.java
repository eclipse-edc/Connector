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

package org.eclipse.edc.connector.dataplane.selector.store.cosmos;

import org.eclipse.edc.azure.cosmos.AbstractCosmosConfig;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Create a config object to interact with a Cosmos database.
 */
public class CosmosDataPlaneInstanceStoreConfig extends AbstractCosmosConfig {

    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    public CosmosDataPlaneInstanceStoreConfig(ServiceExtensionContext context) {
        super(context);
    }

  
    @Override
    protected String getAccountNameSetting() {
        return "edc.dataplaneinstancestore.cosmos.account-name";
    }

    @Override
    protected String getDbNameSetting() {
        return "edc.dataplaneinstancestore.cosmos.database-name";
    }

    @Override
    protected String getCosmosPreferredRegionSetting() {
        return "edc.dataplaneinstancestore.cosmos.preferred-region";
    }

    @Override
    protected String getContainerNameSetting() {
        return "edc.dataplaneinstancestore.cosmos.container-name";
    }
}
