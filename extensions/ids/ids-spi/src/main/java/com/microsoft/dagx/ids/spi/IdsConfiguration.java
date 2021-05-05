/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi;

import com.microsoft.dagx.spi.DagxSetting;

public interface IdsConfiguration {

    /**
     * The unique id of the current connector.
     */
    @DagxSetting
    String CONNECTOR_NAME = "dagx.ids.connector.name";

}
