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
