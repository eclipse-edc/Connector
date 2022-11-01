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

package org.eclipse.edc.connector.store.azure.cosmos.assetindex;

import org.eclipse.edc.azure.cosmos.AbstractCosmosConfig;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class AssetIndexCosmosConfig extends AbstractCosmosConfig {

    @Setting
    private static final String COSMOS_ACCOUNTNAME_SETTING = "edc.assetindex.cosmos.account-name";
    @Setting
    private static final String COSMOS_DBNAME_SETTING = "edc.assetindex.cosmos.database-name";
    @Setting
    private static final String COSMOS_PREFERRED_REGION_SETTING = "edc.assetindex.cosmos.preferred-region";
    @Setting
    private static final String COSMOS_CONTAINER_NAME_SETTING = "edc.assetindex.cosmos.container-name";

    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    protected AssetIndexCosmosConfig(ServiceExtensionContext context) {
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
