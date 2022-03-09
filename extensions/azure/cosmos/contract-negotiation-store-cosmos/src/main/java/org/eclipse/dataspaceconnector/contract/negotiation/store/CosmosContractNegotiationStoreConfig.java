package org.eclipse.dataspaceconnector.contract.negotiation.store;

import org.eclipse.dataspaceconnector.azure.cosmos.AbstractCosmosConfig;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class CosmosContractNegotiationStoreConfig extends AbstractCosmosConfig {
    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    protected CosmosContractNegotiationStoreConfig(ServiceExtensionContext context) {
        super(context);
    }

    @Override
    protected String getAccountNameSetting() {
        return "edc.contractnegotiationstore.cosmos.account-name";
    }

    @Override
    protected String getDbNameSetting() {
        return "edc.contractnegotiationstore.cosmos.database-name";
    }

    @Override
    protected String getCosmosPreferredRegionSetting() {
        return "edc.contractnegotiationstore.cosmos.preferred-region";
    }

    @Override
    protected String getContainerNameSetting() {
        return "edc.contractnegotiationstore.cosmos.container-name";
    }
}
