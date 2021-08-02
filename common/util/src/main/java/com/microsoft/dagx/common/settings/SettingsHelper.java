/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.settings;

import com.microsoft.dagx.common.string.StringUtils;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SettingsHelper {
    private static String connectorId;

    /**
     * Fetches the unique ID of the connector. If the {@code dagx.ids.connector.name} config value has been set, that value
     * is returned, else a random name is chosen.
     * The connector id is non-transient, that means two subsequent calls will produce the same result.
     */
    public static synchronized String getConnectorId(ServiceExtensionContext context) {
        if (StringUtils.isNullOrEmpty(connectorId)) {
            connectorId = context.getSetting("dagx.ids.connector.name", "dagx-connector-" + UUID.randomUUID());
        }
        return connectorId;
    }

    /**
     * Convenience method to either get a particular setting or throw a {@link DagxException}
     */
    @NotNull
    public static String getSettingOrThrow(ServiceExtensionContext serviceContext, String setting) {
        var value = serviceContext.getSetting(setting, null);
        if (value == null) {
            throw new DagxException("could not find setting " + setting + " or it was null");
        }
        return value;
    }
}
