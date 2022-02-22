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
