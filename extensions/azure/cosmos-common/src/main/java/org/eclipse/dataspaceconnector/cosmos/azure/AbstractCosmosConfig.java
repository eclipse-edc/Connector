package org.eclipse.dataspaceconnector.cosmos.azure;

import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;

public abstract class AbstractCosmosConfig {

    @EdcSetting
    private static final String DEFAULT_PARTITION_KEY_SETTING = "edc.cosmos.partition-key";
    @EdcSetting
    private static final String DEFAULT_QUERY_METRICS_ENABLED_SETTING = "edc.cosmos.query-metrics-enabled";

    public static final String DEFAULT_REGION = "westeurope";
    private static final String DEFAULT_PARTITION_KEY = "dataspaceconnector";


    private final String containerName;
    private final String partitionKey;
    private final String accountName;
    private final String preferredRegion;
    private final String dbName;
    private final boolean queryMetricsEnabled;

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
        this.queryMetricsEnabled = Boolean.parseBoolean(context.getSetting(getQueryMetricsEnabledSetting(), "true"));

        assertNotBlank(accountName);
        assertNotBlank(dbName);
        assertNotBlank(partitionKey);
        assertNotBlank(preferredRegion);
    }

    public String getContainerName() {
        return containerName;
    }

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

    public boolean isQueryMetricsEnabled() {
        return queryMetricsEnabled;
    }

    protected abstract String getAccountNameSetting();

    protected abstract String getDbNameSetting();

    protected abstract String getCosmosPreferredRegionSetting();

    protected abstract String getContainerNameSetting();

    protected String getPartitionKeySetting() {
        return DEFAULT_PARTITION_KEY_SETTING;
    }

    protected String getQueryMetricsEnabledSetting() {
        return DEFAULT_QUERY_METRICS_ENABLED_SETTING;
    }

    private static void assertNotBlank(String test) {
        if (StringUtils.isNullOrEmpty(test)) {
            throw new EdcException("'" + test + "' cannot be null or empty!");
        }
    }
}
