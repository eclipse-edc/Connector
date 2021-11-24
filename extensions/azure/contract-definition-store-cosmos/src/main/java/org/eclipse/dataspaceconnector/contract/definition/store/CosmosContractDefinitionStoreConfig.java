package org.eclipse.dataspaceconnector.contract.definition.store;

import org.eclipse.dataspaceconnector.cosmos.azure.AbstractCosmosConfig;
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
        return null;
    }

    @Override
    protected String getDbNameSetting() {
        return null;
    }

    @Override
    protected String getCosmosPreferredRegionSetting() {
        return null;
    }

    @Override
    protected String getContainerNameSetting() {
        return null;
    }
}
