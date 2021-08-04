/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.ids.spi;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public interface IdsConfiguration {

    /**
     * The unique id of the current connector.
     */
    @EdcSetting
    String CONNECTOR_NAME = "dataspaceconnector.ids.connector.name";

}
