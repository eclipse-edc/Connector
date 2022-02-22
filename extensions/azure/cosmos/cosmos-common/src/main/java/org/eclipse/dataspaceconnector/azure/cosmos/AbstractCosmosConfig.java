/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.cosmos;


import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.ArrayList;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.common.string.StringUtils.isNullOrBlank;

public abstract class AbstractCosmosConfig {

    public static final String DEFAULT_REGION = "westeurope";
    @EdcSetting
    private static final String DEFAULT_PARTITION_KEY_SETTING = "edc.cosmos.partition-key";
    @EdcSetting
    private static final String DEFAULT_QUERY_METRICS_ENABLED_SETTING = "edc.cosmos.query-metrics-enabled";
    private static final String DEFAULT_PARTITION_KEY = "dataspaceconnector";
    private static Monitor monitor;


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
        monitor = context.getMonitor();

        accountName = context.getSetting(getAccountNameSetting(), null);
        dbName = context.getSetting(getDbNameSetting(), null);
        partitionKey = context.getSetting(getPartitionKeySetting(), DEFAULT_PARTITION_KEY);
        preferredRegion = context.getSetting(getCosmosPreferredRegionSetting(), DEFAULT_REGION);
        containerName = context.getSetting(getContainerNameSetting(), null);
        queryMetricsEnabled = Boolean.parseBoolean(context.getSetting(getQueryMetricsEnabledSetting(), "true"));

        var errors = new ArrayList<String>();

        if (isNullOrBlank(accountName)) {
            errors.add(getAccountNameSetting());
        }
        if (isNullOrBlank(dbName)) {
            errors.add(getDbNameSetting());

        }
        if (isNullOrBlank(partitionKey)) {
            errors.add(getPartitionKeySetting());
        }
        if (isNullOrBlank(preferredRegion)) {
            errors.add(getCosmosPreferredRegionSetting());
        }

        if (!errors.isEmpty()) {
            var str = format("The following configuration parameters cannot be empty: [%s]", String.join(", ", errors));
            throw new EdcException(str);
        }
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

}
