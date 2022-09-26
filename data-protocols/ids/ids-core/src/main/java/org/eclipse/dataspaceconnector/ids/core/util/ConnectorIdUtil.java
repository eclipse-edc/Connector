/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.util;

import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Utility class for handling the connector's IDS ID.
 */
public class ConnectorIdUtil {
    
    @EdcSetting
    public static final String EDC_IDS_ID = "edc.ids.id";
    public static final String DEFAULT_EDC_IDS_ID = "urn:connector:edc";
    
    private ConnectorIdUtil() {}
    
    /**
     * Returns the connector's ID from its IDS ID. Will use the IDS ID supplied in the configuration,
     * if it is present, else will use a default IDS ID.
     *
     * @param context the context providing the configuration values.
     * @return the connector's ID.
     * @throws EdcException if a configuration value is present for the IDS ID but is not a valid URN.
     */
    public static String resolveConnectorId(ServiceExtensionContext context) {
        var value = context.getSetting(EDC_IDS_ID, DEFAULT_EDC_IDS_ID);
    
        // Hint: use stringified uri to keep uri path and query
        var result = IdsId.from(value);
        if (result.succeeded()) {
            var idsId = result.getContent();
            if (idsId.getType() == IdsType.CONNECTOR) {
                return idsId.getValue();
            }
        } else {
            var message = "IDS Settings: Expected valid URN for setting '%s', but was %s'. Expected format: 'urn:connector:[id]'";
            throw new EdcException(String.format(message, EDC_IDS_ID, DEFAULT_EDC_IDS_ID));
        }
    
        return value;
    }
    
}
