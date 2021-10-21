package org.eclipse.dataspaceconnector.cosmos.azure;

import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;

public abstract class AbstractCosmosConfig {

    public static final String DEFAULT_REGION = "West US";
    private static final String DEFAULT_PARTITION_KEY = "dataspaceconnector";

    private final String containerName;
    private final String partitionKey;
    private final String accountName;
    private final String preferredRegion;
    private final String dbName;

    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    protected AbstractCosmosConfig(ServiceExtensionContext context) {
        Objects.requireNonNull(context);

        this.accountName = context.getSetting(getAccountNameSetting(), null);
        this.dbName = context.getSetting(getDbNameSetting(), null);
        this.partitionKey = context.getSetting(getPartitionKeySetting(), DEFAULT_PARTITION_KEY);
        this.preferredRegion = context.getSetting(getCosmosPreferredRegionSetting(), DEFAULT_REGION);
        this.containerName = context.getSetting(getContainerNameSetting(), null);

        assertNotBlank(accountName);
        assertNotBlank(dbName);
        assertNotBlank(partitionKey);
        assertNotBlank(preferredRegion);
    }

    public String getContainerName() {
        return containerName;
    }

    /**
     * This is the partition key that CosmosDB uses for r/w distribution. Contrary to what CosmosDB suggests, this
     * key should be the same for all local (=clustered) connectors, otherwise queries in stored procedures might
     * produce incomplete results.
     */
    public String getPartitionKey() {
        return partitionKey;
    }

    public String getPreferredRegion() {
        return preferredRegion;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getDbName() {
        return dbName;
    }

    protected abstract String getAccountNameSetting();

    protected abstract String getDbNameSetting();

    protected abstract String getPartitionKeySetting();

    protected abstract String getCosmosPreferredRegionSetting();

    protected abstract String getContainerNameSetting();

    private static void assertNotBlank(String test) {
        if (StringUtils.isNullOrEmpty(test)) {
            throw new EdcException("'" + test + "' cannot be null or empty!");
        }
    }
}
