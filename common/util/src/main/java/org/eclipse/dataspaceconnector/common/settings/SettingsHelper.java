/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.common.settings;

import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SettingsHelper {
    private static String connectorId;

    /**
     * Fetches the unique ID of the connector. If the {@code dataspaceconnector.ids.connector.name} config value has been set, that value
     * is returned, else a random name is chosen.
     * The connector id is non-transient, that means two subsequent calls will produce the same result.
     */
    public static synchronized String getConnectorId(ServiceExtensionContext context) {
        if (StringUtils.isNullOrEmpty(connectorId)) {
            connectorId = context.getSetting("dataspaceconnector.ids.connector.name", "dataspaceconnector-connector-" + UUID.randomUUID());
        }
        return connectorId;
    }

    /**
     * Convenience method to either get a particular setting or throw a {@link EdcException}
     */
    @NotNull
    public static String getSettingOrThrow(ServiceExtensionContext serviceContext, String setting) {
        var value = serviceContext.getSetting(setting, null);
        if (value == null) {
            throw new EdcException("could not find setting " + setting + " or it was null");
        }
        return value;
    }
}
