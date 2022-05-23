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

package org.eclipse.dataspaceconnector.transfer.store.cosmos;

import org.eclipse.dataspaceconnector.azure.cosmos.AbstractCosmosConfig;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class TransferProcessStoreCosmosConfig extends AbstractCosmosConfig {

    @EdcSetting
    private static final String COSMOS_ACCOUNTNAME_SETTING = "edc.transfer-process-store.cosmos.account.name";

    @EdcSetting
    private static final String COSMOS_DBNAME_SETTING = "edc.transfer-process-store.database.name";

    @EdcSetting
    private static final String COSMOS_PREFERRED_REGION_SETTING = "edc.transfer-process-store.cosmos.preferred-region";

    @EdcSetting
    private static final String COSMOS_CONTAINER_NAME_SETTING = "edc.transfer-process-store.cosmos.container-name";

    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    protected TransferProcessStoreCosmosConfig(ServiceExtensionContext context) {
        super(context);
    }

    /**
     * Boolean setting to allow or disallow auto-uploading any stored procedures that this extension needs.
     * Disable to reduce startup times.
     *
     * @return the key of the setting
     */
    public String allowSprocAutoUploadSetting() {
        return "edc.transfer-process-store.cosmos.allow.sproc.autoupload";
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
