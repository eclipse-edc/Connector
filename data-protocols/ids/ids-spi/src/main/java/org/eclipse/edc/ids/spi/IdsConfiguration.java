/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.ids.spi;

import org.eclipse.edc.spi.EdcSetting;

public interface IdsConfiguration {

    /**
     * The unique id of the current connector.
     */
    @EdcSetting
    String CONNECTOR_NAME = "edc.ids.connector.name";

}
