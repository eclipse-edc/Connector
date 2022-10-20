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

package org.eclipse.edc.catalog.node.directory.azure;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.azure.cosmos.AbstractCosmosConfig;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class FederatedCacheNodeDirectoryCosmosConfig extends AbstractCosmosConfig {

    @Setting
    private static final String COSMOS_ACCOUNTNAME_SETTING = "edc.node.directory.cosmos.account.name";
    @Setting
    private static final String COSMOS_DBNAME_SETTING = "edc.node.directory.cosmos.database.name";
    @Setting
    private static final String COSMOS_PREFERRED_REGION_SETTING = "edc.node.directory.cosmos.preferred.region";
    @Setting
    private static final String COSMOS_CONTAINER_NAME_SETTING = "edc.node.directory.cosmos.container.name";

    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    protected FederatedCacheNodeDirectoryCosmosConfig(ServiceExtensionContext context) {
        super(context);
    }

    @Override
    protected String getAccountNameSetting() {
        return COSMOS_ACCOUNTNAME_SETTING;
    }

    @Override
    protected String getDbNameSetting() {
        return COSMOS_DBNAME_SETTING;
    }

    @Override
    protected String getCosmosPreferredRegionSetting() {
        return COSMOS_PREFERRED_REGION_SETTING;
    }

    @Override
    protected String getContainerNameSetting() {
        return COSMOS_CONTAINER_NAME_SETTING;
    }
}
