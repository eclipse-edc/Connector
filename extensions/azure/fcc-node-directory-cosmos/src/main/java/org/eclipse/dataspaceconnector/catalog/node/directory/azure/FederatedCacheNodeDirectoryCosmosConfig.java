package org.eclipse.dataspaceconnector.catalog.node.directory.azure;

import org.eclipse.dataspaceconnector.cosmos.azure.AbstractCosmosConfig;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class FederatedCacheNodeDirectoryCosmosConfig extends AbstractCosmosConfig {

    @EdcSetting
    private static final String COSMOS_ACCOUNTNAME_SETTING = "edc.node.directory.cosmos.account.name";
    @EdcSetting
    private static final String COSMOS_DBNAME_SETTING = "edc.node.directory.cosmos.database.name";
    @EdcSetting
    private static final String COSMOS_PARTITION_KEY_SETTING = "edc.node.directory.cosmos.partition.key";
    @EdcSetting
    private static final String COSMOS_PREFERRED_REGION_SETTING = "edc.node.directory.cosmos.preferred.region";
    @EdcSetting
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
    protected String getPartitionKeySetting() {
        return COSMOS_PARTITION_KEY_SETTING;
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
